# Backend reference

The Firestore and Storage data model that the Android app must read and write. It describes the backend
as the existing clients use it today. Nothing here is a proposal.

Sources, read on 2026-10-03:

- Web app (`crlsribeiro/home-market-app`): `src/types/index.ts`, `src/hooks/useAuth.ts`,
  `src/hooks/useHousehold.ts`, `src/hooks/useList.ts`, `src/hooks/useHistory.ts`, `src/lib/utils.ts`,
  `src/App.tsx`, `functions/src/index.ts`.
- iOS app (`crlsribeiro/home-market-app-ios`): `HomeMarket/Models`, `HomeMarket/Services`,
  `HomeMarket/ViewModels`, `HomeMarket/Views`.

When the two clients disagree, the web app's behaviour is the contract (see `AGENTS.md`). Every difference
is listed in [Differences between the web and iOS clients](#differences-between-the-web-and-ios-clients).

Neither repository contains `firestore.rules`, `storage.rules` or `firestore.indexes.json`. The repository
owner will export the deployed rules and indexes from the Firebase console; they will be stored in `docs/`
as a read-only reference. The Android app never changes them.

## Decisions for the Android client

Decided by the repository owner on 2026-10-03. Each one writes only fields and values that already exist.

1. **`weekLabel` is written in Portuguese, like the web.** Format and month names are in
   [Week calculation](#week-calculation). The UI never shows or parses the stored text: it formats the
   label from `weekStart` (and `weekEnd`) with the device locale. Purchases have no `weekStart`, so the
   history screen formats the label from the linked `lists/{listId}` and falls back to the stored
   `purchases.weekLabel` only when that list cannot be read.
2. **List document ids are deterministic, in the exact iOS format.** See
   [List document id](#list-document-id).
3. **"Item not found" follows the web.** The admin writes `status: "not_found"` (which triggers
   `onItemNotFound`). The member who added the item later resolves it, which writes `status: "rolled_over"`
   and `notFoundResolved: true`.
4. **Prices follow the web: admin only.** Members never see the History tab, purchase totals, line prices
   or the price in item detail.
5. **The `onItemAdded` defect is recorded in [`backend-proposals.md`](backend-proposals.md).** The owner
   fixes it in the web repository. Android does not write `householdMembers`.
6. **Same business rules as the existing apps.** Android keeps the iOS rule for every feature except the
   ones above that follow the web. It adds no rule of its own. In particular:
   - The list status flow offers both the web lock step (`open → locked → shopping`) and the iOS shortcut
     (`open → shopping`), plus abandon shopping (`shopping → open`).
   - Adding an item when there is no current list creates this week's list first, as on iOS.
   - A current list whose `weekEnd` has passed is closed with the weekly cut, without a purchase, and this
     week's list is created, as on iOS.
   - `rolled_over` items stay as they are today: they keep their `listId` and are shown as "next week"
     items. Nothing moves them into the new list.
   - A list closed before Sunday blocks a new list until the next Monday, as on iOS (see
     [List document id](#list-document-id)).
7. **Sign in with Apple is not offered on Android.** Android writes no Apple-specific field, so nothing in
   the data changes.

## Firebase project

- Project: `home-market-3d9da` (from `src/lib/firebase.ts`).
- Storage bucket: `home-market-3d9da.firebasestorage.app`.
- Products in use: Authentication, Cloud Firestore, Cloud Storage, Cloud Messaging, Cloud Functions (v2).
- Auth providers in use: email/password, Google. The iOS app also uses Sign in with Apple.

## Collections

All collections are top-level. Timestamps are Firestore `Timestamp` values.

### `users/{uid}`

The document id is the Firebase Auth uid.

| Field | Type | Written by | Notes |
|---|---|---|---|
| `uid` | string | web, iOS | Same value as the document id. |
| `displayName` | string | web, iOS | Default when Auth has no name: web `"Usuário"` (auth listener) or `"Usuario"` (`signUpWithEmail`), iOS `"User"`. Registration writes `"{firstName} {lastName}"`. |
| `email` | string | web, iOS | iOS also re-syncs it from the live Auth email in `refreshAppUser`. |
| `photoURL` | string or null | web, iOS | Download URL of `users/{uid}/avatar`, or the Google profile photo URL. |
| `householdId` | string or null | web, iOS | `null` until the user creates or joins a household. One household per user. |
| `role` | `"admin"` or `"member"` | web, iOS | Default `"member"`. Set to `"admin"` only when the user creates a household (or, on iOS, when promoted during an admin's account deletion). Readers treat a missing or unknown value as `"member"`. |
| `joinedAt` | timestamp | web, iOS | `serverTimestamp()` on creation. |
| `fcmToken` | string | web, iOS | One token per user. Every sign-in overwrites it. See [Push notifications](#push-notifications). |
| `firstName` | string | web, iOS | Only written by the full registration form. |
| `lastName` | string | web, iOS | Only written by the full registration form. |
| `phone` | string | web, iOS | Registration form. iOS can also update it in Account settings. Stored formatted, for example `(11) 91234-5678`. Can be empty. |
| `phoneCountryCode` | string | web, iOS | Dial code, for example `"+55"`. |
| `provider` | string | web, iOS | `"email"` from the registration form. Not written for Google or Apple sign-in. |

Writes:

- **First sign-in (any provider):** if `users/{uid}` does not exist, `setDoc` with `uid`, `displayName`,
  `email`, `photoURL`, `householdId: null`, `role: "member"`, `joinedAt: serverTimestamp()`.
- **Registration form (email/password):** create the Auth user, set the Auth profile `displayName`, then
  `setDoc` with every field above except `fcmToken`. The avatar is uploaded afterwards and `photoURL` is
  updated. iOS writes the document first and uploads the avatar second, so a failed upload does not fail
  the registration. The web uploads first.
- **Avatar change (iOS Account settings):** upload to `users/{uid}/avatar`, update the Auth profile
  `photoURL` and `users/{uid}.photoURL`.
- **Phone change (iOS Account settings):** `updateDoc` `phone`, `phoneCountryCode`.
- **Email change (iOS Account settings):** `verifyBeforeUpdateEmail` on Auth only. `users/{uid}.email` is
  reconciled on the next `refreshAppUser`.
- **FCM token:** `updateDoc` `fcmToken` after each sign-in when notification permission is granted.
- **Account deletion (iOS only):** leave the household (see below), `delete` `users/{uid}`, then delete the
  Auth user. Items, lists and purchases the user created are not touched.

Reads:

- `getDoc(users/{uid})` on every auth state change.
- `getDoc(users/{uid})` for each uid in `households.memberUids` to show the member list (a fan-out, not a
  query).

### `households/{householdId}`

| Field | Type | Notes |
|---|---|---|
| `name` | string | Household name entered at creation. |
| `adminUid` | string | The single admin. |
| `inviteToken` | string | 8 characters from `[A-Za-z0-9]`. |
| `memberUids` | array of string | Includes the admin. |
| `createdAt` | timestamp | Web writes the client `Date`; iOS writes `serverTimestamp()`. |

The document id is not a Firestore auto-id. It is a random 20-character string from the same
`[A-Za-z0-9]` alphabet as the invite token (`generateToken(20)` in `src/lib/utils.ts`).

Writes:

- **Create:** `setDoc` with `name`, `adminUid: uid`, `inviteToken`, `memberUids: [uid]`, `createdAt`. Then
  `updateDoc(users/{uid})` with `householdId` and `role: "admin"`.
- **Join:** query `households` where `inviteToken == token.trim()`. No match means "invalid code". If the uid
  is not in `memberUids` yet, add it (web rewrites the array; iOS uses `arrayUnion`). Then
  `updateDoc(users/{uid})` with `householdId` and `role: "member"`. Joining again resets the role to
  `"member"`, even for a former admin.
- **New invite token:** `updateDoc` `inviteToken` with a new 8-character token. The old token stops working.
- **Leave (iOS account deletion only):** if the leaving user is the admin and other members remain, the
  first other member becomes admin: `updateDoc(households)` `adminUid`, then `updateDoc(users/{newAdmin})`
  `role: "admin"`. Then `arrayRemove(uid)` from `memberUids`. A household with no members left is not
  deleted.

Reads:

- Snapshot listener on `households/{householdId}`.
- One-off query `where inviteToken == token` when joining.

### `lists/{listId}`

| Field | Type | Notes |
|---|---|---|
| `householdId` | string | |
| `weekLabel` | string | Display label such as `"28 – 4 OUT"`. See [Week calculation](#week-calculation). |
| `weekStart` | timestamp | Monday 00:00:00.000, device time zone. |
| `weekEnd` | timestamp | Sunday 23:59:59.999 (web) or 23:59:59 (iOS), device time zone. |
| `status` | `"open"`, `"locked"`, `"shopping"`, `"closed"` | |
| `createdAt` | timestamp | `serverTimestamp()`. |
| `closedAt` | timestamp or null | `null` until closed. |

Status lifecycle:

```
open ──lock──▶ locked ──start shopping──▶ shopping ──weekly cut──▶ closed
  ▲                                           │
  └────────────── abandon shopping (iOS) ─────┘
```

- Web: admin panel offers "lock" (`open → locked`) and then "start shopping" (`locked → shopping`). The
  admin can also run the weekly cut from the main list ("Fazer corte") in any active status.
- iOS: the admin goes straight from `open` (or `locked`) to `shopping` with the cart button. iOS never
  writes `locked`. iOS also lets the admin abandon shopping (`shopping → open`).
- `closed` is only written by the weekly cut (or by `updateListStatus("closed")`, which also sets
  `closedAt`).

The **current list** is the newest list of the household (by `createdAt`) whose status is `open`,
`locked` or `shopping`. Both clients listen to `lists where householdId == X` and filter and sort on the
device. There is no "current" flag in the data.

Writes:

- **Create:** `status: "open"`, week fields from the current date, `createdAt: serverTimestamp()`,
  `closedAt: null`. Web uses `addDoc` (auto-id). iOS uses a deterministic id
  `{householdId}_{yyyy-MM-dd of weekStart}` inside a transaction that only writes when the document does not
  exist yet, so two devices creating the week's list at the same time end up on the same document.
- **Status change:** `updateDoc` `status` (plus `closedAt: serverTimestamp()` when the new status is
  `closed`).
- **Weekly cut:** see [Weekly cut](#weekly-cut).

#### List document id

Android uses the iOS format (`ListService.createList` and `ListService.listId` in the iOS app):

```
{householdId}_{yyyy-MM-dd}
```

- `householdId`: the household document id, unchanged (20 characters from `[A-Za-z0-9]`).
- `_`: one underscore.
- `yyyy-MM-dd`: the date of `weekStart` (the Monday of the week) in the Gregorian calendar and the device
  time zone, with a 4-digit year and zero-padded month and day, in ASCII digits.
- Example: household `aB3dE5fG7hJ9kL1mN2pQ`, week of Monday 28 September 2026 →
  `aB3dE5fG7hJ9kL1mN2pQ_2026-09-28`.

On Android, format the date with a fixed locale (`Locale.ROOT` or `Locale.US`) so the digits are always
ASCII. iOS formats with the device locale, which gives the same string for every locale that uses Latin
digits.

Create it in a transaction: read `lists/{id}`; if it exists, write nothing; otherwise `set` the fields
listed above (`status: "open"`, `createdAt: serverTimestamp()`, `closedAt: null`). Then read the document
back. Two devices that create the same week's list at the same time land on the same document.

Lists created by the web keep their auto-ids. The current list is still found by the query above, never by
building the id, so both kinds of id work side by side. Do not use the id to decide which list is current:
a list closed early in the week and a new list created the same week would share the id. The transaction
then finds the closed list and writes nothing, so the household has no current list until next Monday.
iOS has the same limitation. See [Open questions](#open-questions).

### `items/{itemId}`

| Field | Type | Notes |
|---|---|---|
| `listId` | string | List the item was added to. |
| `householdId` | string | |
| `name` | string | Trimmed, not empty (iOS). |
| `quantity` | number | Integer in practice. iOS limits it to 1–99. |
| `notes` | string | Empty string when there are no notes. |
| `urgent` | boolean | |
| `addedByUid` | string | |
| `addedByName` | string | Display name at the time the item was added. |
| `status` | `"pending"`, `"purchased"`, `"not_found"`, `"rolled_over"` | |
| `approvalStatus` | `"not_required"`, `"pending"`, `"approved"`, `"rejected"` | |
| `notFoundResolved` | boolean | |
| `photoURL` | string or null | Download URL of the item photo. |
| `createdAt` | timestamp | `serverTimestamp()`. |

Writes:

- **Add:** create with an auto-id, `status: "pending"`,
  `approvalStatus: list.status == "open" ? "not_required" : "pending"`, `notFoundResolved: false`,
  `photoURL: null`, `createdAt: serverTimestamp()`. If there is a photo, upload it to
  `households/{householdId}/items/{itemId}/photo` and then `updateDoc` `photoURL`. On iOS, adding an item
  when there is no current list creates this week's list first.
- **Toggle purchased:** `status` becomes `"pending"` if it was `"purchased"`, otherwise `"purchased"`.
- **Mark not found:** `status: "not_found"`. This triggers the `onItemNotFound` Cloud Function.
- **Resolve not found:** `status: "rolled_over"`, `notFoundResolved: true`.
- **Approve:** `approvalStatus: "approved"`. `status` stays `"pending"`.
- **Reject:** `approvalStatus: "rejected"`, `status: "rolled_over"`.
- **Edit notes:** `updateDoc` `notes`.
- **Replace photo (iOS item detail):** upload to the same Storage path, then `updateDoc` `photoURL`.
- **Delete:** `deleteDoc`, after a confirmation step in item detail. iOS also offers swipe-to-delete on
  the list and hides delete when the list is closed.

Neither client limits note edits or deletion to the item's author or to the admin: any household member
can edit or delete any item.

Reads:

- Current list items: snapshot listener on `items where listId == currentList.id`, then on the device keep
  `status in [pending, purchased, not_found]` and sort by `createdAt` ascending.
- "Next week" items: snapshot listener on
  `items where householdId == X and status == "rolled_over"`, sorted by `createdAt` ascending on the device.
  This query has two equality filters, which Firestore serves without a composite index.

Derived groups used by the screens:

- Pending to buy: `status == "pending" && approvalStatus != "pending"`.
- Awaiting approval: `approvalStatus == "pending"` (web also requires the list to be `locked` or
  `shopping`).
- Purchased: `status == "purchased"`.
- Not found: `status == "not_found"`.
- Urgent: `urgent && status != "purchased"`.
- A member's unresolved not-found items (web): `status == "not_found" && addedByUid == uid &&
  !notFoundResolved`.

#### Weekly cut

One `WriteBatch`:

1. `lists/{listId}`: `status: "closed"`, `closedAt: serverTimestamp()`.
2. Every item of the current list with `status == "pending"` and `approvalStatus != "pending"`:
   `status: "rolled_over"`.

Items awaiting approval keep `status: "pending"` on a closed list. After the batch commits, both clients
create the purchase record for the list (see `purchases`). These two steps are not atomic.

iOS also runs the weekly cut automatically, without creating a purchase, when the current list's
`weekEnd` is in the past. It then creates this week's list.

#### Rolled-over items

`rolled_over` items keep their original `listId`. No client moves them into the next list or changes their
status back to `pending`. They stay in the "next week" section until someone deletes them.

### `purchases/{purchaseId}`

| Field | Type | Notes |
|---|---|---|
| `listId` | string | The closed list. |
| `householdId` | string | |
| `weekLabel` | string | Copied from the list. |
| `total` | number | Sum of `purchaseItems.totalPrice`, rounded to 2 decimals. |
| `receiptUrl` | string or null | Download URL of the receipt photo. |
| `receiptProcessed` | boolean | `true` after a receipt was uploaded and parsed. |
| `createdAt` | timestamp | Client time (`new Date()` / `Timestamp(date:)`), not `serverTimestamp()`. |
| `storeName` | string or null | iOS only. First line of the receipt text that contains a letter and is at most 40 characters. Not in the web types. |

Writes:

- **Create after weekly cut:** `addDoc` with `total: 0`, `receiptUrl: null`, `receiptProcessed: false`.
  iOS first queries `purchases where listId == X limit 1` and reuses the existing purchase, so closing the
  same list twice does not create two records. The web always creates a new one.
- **Receipt upload:** see `purchaseItems`. Then `updateDoc` `receiptUrl`, `receiptProcessed: true`,
  `total` (and on iOS `storeName`).
- **Price edit:** `updateDoc` `total` after recomputing the sum.

Reads: snapshot listener on `purchases where householdId == X`, sorted by `createdAt` descending on the
device.

### `purchaseItems/{purchaseItemId}`

| Field | Type | Notes |
|---|---|---|
| `purchaseId` | string | |
| `name` | string | |
| `quantity` | number | Always written as `1` by receipt parsing. |
| `unitPrice` | number | |
| `totalPrice` | number | `unitPrice * quantity`, rounded to 2 decimals. |

Writes:

- **Receipt upload:** upload the image, run OCR **on the device**, parse the lines, delete every existing
  `purchaseItems where purchaseId == X`, then `addDoc` one document per parsed line. There is no backend
  function involved: the web uses tesseract.js and iOS uses Apple Vision. When nothing is recognized, the
  web writes a single placeholder line with price 0 (`"Nenhum item reconhecido (Clique no lápis para
  adicionar)"`); iOS writes the same kind of line in English (`"No items recognized (tap the pencil to add
  one)"`, `HistoryService.swift`). Android writes the iOS line.
- **Price edit:** `updateDoc` `unitPrice`, `totalPrice` and optionally `name`, then recompute the
  purchase `total`.

Reads: one-off query `purchaseItems where purchaseId == X`.

Receipt parsing rules (shared by both clients, iOS is more tolerant):

- Stop at the first line containing `subtotal`, `total sale`, `tax`, `visa`, `mastercard`, `cash`,
  `change`, `items purchased` or `account #`.
- A line ending in a price `\d+\.\d{2}` is an item; remove a leading index number and a trailing tax code.
- Ignore prices `<= 0` or `> 500`.
- iOS also handles weight lines (`... @ ...`), whose last number is the price of the item name on the line
  above, and lines with more than one price.
- The parser targets H-E-B (US) receipts and USD prices.

### `householdMembers` (read only, not written by any client)

The `onItemAdded` Cloud Function reads `householdMembers where householdId == X` and expects a `userId`
field. Neither client writes this collection. Membership lives in `households.memberUids`. See
[Push notifications](#push-notifications).

## Storage

| Path | Content | Written by |
|---|---|---|
| `users/{uid}/avatar` | Profile photo. Overwritten on change. | web, iOS |
| `households/{householdId}/items/{itemId}/photo` | Item photo. Overwritten on change. | web, iOS |
| `receipts/{purchaseId}/{fileName}` | Receipt photo. Web uses the original file name; iOS uses `{UUID}.jpg`. Old receipts are not deleted. | web, iOS |

Clients store the download URL (`getDownloadURL`) in Firestore, not the path.

## Week calculation

From `src/lib/utils.ts`, in the device time zone:

- `weekStart`: Monday of the current week at 00:00:00.000. Sunday belongs to the week that started six
  days earlier.
- `weekEnd`: `weekStart + 6 days` at 23:59:59.999.
- `weekLabel`: `"{day of weekStart} – {day of weekEnd} {MONTH of weekEnd}"`: days without zero padding,
  a space, an en dash (U+2013), a space, and the month abbreviation of `weekEnd`. Web months:
  `JAN FEV MAR ABR MAI JUN JUL AGO SET OUT NOV DEZ`. iOS months:
  `JAN FEB MAR APR MAY JUN JUL AUG SEP OCT NOV DEC`.
- Android writes the web months. Example: week of Monday 28 September 2026 → `"28 – 4 OUT"`.

## Push notifications

FCM tokens are stored in **`users/{uid}.fcmToken`**, one string per user. The web writes it with a VAPID
key after `Notification.requestPermission()`; iOS writes it after APNs registration. Each sign-in on any
device overwrites the previous token, so only the last device a user signed in on receives
notifications. Nothing removes the token on sign-out.

Cloud Functions (`functions/src/index.ts`, Firebase Functions v2):

| Function | Trigger | Recipients | Message (pt-BR) |
|---|---|---|---|
| `onItemAdded` | `onDocumentCreated("items/{itemId}")` | Every member of `item.householdId` except `item.addedByUid`, looked up in `householdMembers` | Title `🛒 Novo item na lista!`, body `{addedByName} adicionou "{name}" na lista` |
| `onItemNotFound` | `onDocumentUpdated("items/{itemId}")` when `status` changes to `"not_found"` | `users/{addedByUid}.fcmToken` | Title `😕 Item não encontrado`, body `"{name}" não foi encontrado no mercado. O que fazemos?` |

Both send a `notification` payload only, with no `data` payload (no list id or item id), so a tap cannot
open a specific screen from the message content alone.

## Contradictions with AGENTS.md

1. **Lifecycle "open → locked → shopping → closed".** True for the web. iOS skips `locked`, and can go from
   `shopping` back to `open` (abandon shopping). Android should follow the web and may also offer abandon,
   because it only writes an existing status value.
2. **"Members never see prices."** The web hides the History tab from members and shows prices in item
   detail only to the admin. iOS shows the History tab, with totals and line prices, to every member, and
   hides only the item-detail price from members. AGENTS.md matches the web. **Decision:** follow the web
   (admin only).
3. **M5 "Resolve not-found items (rolled_over + notFoundResolved)".** On the web the admin marks an item
   `not_found` (which notifies the person who added it), and that person later resolves it from a
   "not found" modal. On iOS, the admin's "Not available" button writes `rolled_over` +
   `notFoundResolved: true` directly and never writes `not_found`, so `onItemNotFound` never fires from iOS.
   **Decision:** follow the web.
4. **M6 "register the device token where the Cloud Functions read it".** `onItemNotFound` reads
   `users/{uid}.fcmToken`, which works. `onItemAdded` reads the `householdMembers` collection, which no
   client writes, so it finds no recipients today. A single `fcmToken` field also cannot hold more than one
   device. Both points need a backend change. The `householdMembers` defect is proposal 1 in
   [`backend-proposals.md`](backend-proposals.md), and the owner fixes it in the web repository. The
   one-token-per-user limit is left for M6, which writes its own proposal if it needs one.
5. **M7 "If price extraction depends on a backend function, call the existing one".** It does not: both
   clients run OCR on the device. Android needs an on-device OCR library (for example ML Kit text
   recognition) and the same parser.
6. **`users` fields.** AGENTS.md lists six fields. The data also has `uid`, `fcmToken`, `firstName`,
   `lastName`, `phone`, `phoneCountryCode` and `provider` (iOS reads all of them; the web registration form
   writes them).
7. **`purchases.storeName`.** Written by iOS, not in the web types. Not in AGENTS.md.
8. **`householdMembers`.** Not mentioned in AGENTS.md; read by a Cloud Function.
9. **"Items with `status == rolled_over` are shown as next week items."** True, but nothing ever brings them
   back into a list, so they accumulate. AGENTS.md does not say what "next week" should do.
10. **"Members can edit notes on and delete their own items while the list is open" (M3).** Neither client
    checks ownership: any member can edit or delete any item, in any list status except `closed` on iOS.
    Restricting it on Android is a client-side rule only (the deployed security rules are unknown), so it
    does not change the backend contract.

## Differences between the web and iOS clients

| Topic | Web | iOS |
|---|---|---|
| List document id | Auto-id (`addDoc`) | `{householdId}_{yyyy-MM-dd}` in a create-if-missing transaction |
| `weekLabel` month names | Portuguese (`OUT`, `DEZ`) | English (`OCT`, `DEC`) |
| `weekEnd` time | 23:59:59.999 | 23:59:59 |
| `locked` status | Written by the admin panel | Never written; open → shopping directly |
| Abandon shopping | Not available | `shopping → open` |
| Stale list | Stays current until closed | Auto weekly cut (no purchase) when `weekEnd` has passed, then a new list |
| Add item with no list | Admin must create the list first | Creates this week's list automatically |
| Not found during shopping | `markNotFound` → `not_found`, push to the person who added it | Writes `rolled_over` + `notFoundResolved: true` directly; no push |
| Resolve not found | Member modal "keep" / "discard" (both write the same `resolveNotFound`) | No member-side flow |
| Urgent toggle in add item | Only shown when the list is `locked` or `shopping` | Always shown |
| History tab | Admin only | Every member |
| Purchase on weekly cut | Always creates a new purchase | Reuses an existing purchase for the same `listId` |
| `purchases.storeName` | Not written | Written from OCR |
| OCR | tesseract.js | Apple Vision, smarter line grouping |
| Empty OCR result | Writes one placeholder line at price 0, in Portuguese | Writes one placeholder line at price 0, in English |
| Receipt file name | Original file name | `{UUID}.jpg` |
| `households.createdAt` | Client `Date` | `serverTimestamp()` |
| `memberUids` on join | Rewrites the array | `arrayUnion` |
| Default `displayName` | `"Usuário"` / `"Usuario"` | `"User"` |
| Registration avatar | Uploaded before the user document | Uploaded after the user document; failure is ignored |
| Sign in with Apple | No | Yes |
| Account settings (phone, email, avatar) | No | Yes |
| Account deletion | No | Yes, with admin hand-off |
| Item photo replace after creation | No | Yes |
| Admin "items per person" chart | No | Yes |

## Open questions

1. **Security rules and indexes.** Waiting for the export from the Firebase console. Once received, store
   them in `docs/` unchanged and check every Android write against them.
2. **Adding an item after the week's list was closed early.** iOS then reuses the closed list's id, so the
   new item is written to the closed list and no one sees it. Copying that would lose items. Proposal:
   Android shows "no active list" and disables adding until next Monday, which writes nothing new.
   Waiting for the owner's confirmation. Until then, the Android "create this week's list" action
   follows the proposal: when the transaction finds this week's list already closed, it writes nothing
   and tells the admin that a new list can be created on Monday.

## Resolved questions

Answered by the repository owner on 2026-10-03 and recorded in
[Decisions for the Android client](#decisions-for-the-android-client):

- `weekLabel` language: Portuguese, like the web; the UI formats the label from `weekStart`.
- List document id: deterministic, in the iOS format.
- "Item not found" and prices: follow the web.
- Locked status, abandon shopping, rolled-over items and a second list in the same week: same business
  rules as the existing apps (decision 6).
- Sign in with Apple: not on Android (decision 7).
