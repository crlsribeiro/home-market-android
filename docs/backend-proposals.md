# Backend proposals

Backend changes the Android app needs or has found, written here instead of implemented, as `AGENTS.md`
requires. The Android repository never changes Cloud Functions, security rules or the data model. The
repository owner decides and makes each change in the web repository (`crlsribeiro/home-market-app`).

| # | Title | Status |
|---|---|---|
| 1 | `onItemAdded` reads a collection that no client writes | Accepted; the owner fixes it in the web repository |
| 2 | One FCM token per user: only the last device gets notifications | Proposed (M6) |

## 1. `onItemAdded` reads a collection that no client writes

**Status:** accepted on 2026-10-03. The owner fixes it in the web repository, separately from this project.

### Problem

`onItemAdded` (`functions/src/index.ts`) should notify every household member except the person who added
the item. It finds the recipients with the helper `getTokensExcept`:

```ts
const membersSnap = await admin.firestore()
  .collection("householdMembers")
  .where("householdId", "==", householdId)
  .get();
// for each doc: skip memberData.userId === excludeUid,
// then read users/{memberData.userId}.fcmToken
```

No client writes a `householdMembers` collection. The web (`useHousehold.ts`) and iOS
(`HouseholdService.swift`) both keep membership in `households/{householdId}.memberUids`. The query
returns no documents, so the function sends nothing: today, no one gets an "item added" notification.

`onItemNotFound` is not affected. It reads `users/{addedByUid}.fcmToken` directly.

### Proposed change

Read the members from the household document, where the clients already write them:

```ts
async function getTokensExcept(householdId: string, excludeUid: string): Promise<string[]> {
  const household = await admin.firestore().collection("households").doc(householdId).get();
  const memberUids: string[] = household.data()?.memberUids ?? [];

  const tokens: string[] = [];
  for (const uid of memberUids) {
    if (uid === excludeUid) continue;
    const userDoc = await admin.firestore().collection("users").doc(uid).get();
    const fcmToken = userDoc.data()?.fcmToken;
    if (fcmToken) tokens.push(fcmToken);
  }
  return tokens;
}
```

The change touches only the function. Collections, fields and client code stay the same.

### Impact on Android

- Android does not write `householdMembers` and needs no workaround.
- Until the fix is deployed, "item added" notifications do not arrive on any client. M6 can still be built
  and tested with "item not found", which works today.

### Related limitation

`users/{uid}.fcmToken` holds one token, so only the last device a user signed in on gets notifications.
See proposal 2.

## 2. One FCM token per user: only the last device gets notifications

**Status:** proposed on 2026-10-05, from milestone M6. Waiting for the owner's decision.

### Problem

The clients store the device token in one string field, `users/{uid}.fcmToken` (docs/backend.md, "Push
notifications"):

- the web writes it after `Notification.requestPermission()`;
- iOS writes it after APNs registration (`PushNotificationService.requestAuthorizationAndRegister`);
- every sign-in on any device **overwrites** the previous token;
- nothing removes it on sign-out.

`onItemNotFound` sends to that one token. With Android, the same person often has two or three
devices (Android phone, iPhone, web). Only the device where they signed in last gets the "item not
found" push. The others never do, and signing in on a shared device sends that person's notifications
to it until someone else signs in there.

CODEX_PLAN M6 asks to "register the device token where the Cloud Functions read it, refresh it on
change, remove it on sign-out". With one field, removing the token on sign-out from one device also
silences the device that is still signed in, so the plan cannot be followed as written.

### Proposed change

Keep a list of tokens per user, and keep the old field during the migration.

1. **Data:** add `users/{uid}.fcmTokens: string[]`.
   - On sign-in and on token refresh, each client writes
     `fcmTokens: arrayUnion(token)` and, for older function versions, `fcmToken: token`.
   - On sign-out, the client writes `fcmTokens: arrayRemove(token)` and deletes the device token
     (`FirebaseMessaging.deleteToken()` on Android).
2. **Functions:** read `fcmTokens` (falling back to `fcmToken` when the list is missing), send with
   `sendEachForMulticast`, and remove from `fcmTokens` the tokens the response marks as
   `messaging/registration-token-not-registered` or `messaging/invalid-registration-token`.

```ts
async function getUserTokens(uid: string): Promise<string[]> {
  const data = (await admin.firestore().collection("users").doc(uid).get()).data() ?? {};
  const tokens: string[] = data.fcmTokens ?? (data.fcmToken ? [data.fcmToken] : []);
  return [...new Set(tokens)];
}

async function sendToTokens(uid: string, tokens: string[], notification: admin.messaging.Notification) {
  if (tokens.length === 0) return;
  const response = await admin.messaging().sendEachForMulticast({ tokens, notification });
  const stale = response.responses
    .map((r, i) => (r.error && ["messaging/registration-token-not-registered",
      "messaging/invalid-registration-token"].includes(r.error.code) ? tokens[i] : null))
    .filter((t): t is string => t !== null);
  if (stale.length > 0) {
    await admin.firestore().collection("users").doc(uid)
      .update({ fcmTokens: admin.firestore.FieldValue.arrayRemove(...stale) });
  }
}
```

`getTokensExcept` from proposal 1 would collect `getUserTokens(uid)` for each member instead of one
`fcmToken`.

3. **Optional, same change:** add a `data` payload to both functions (`type`, `householdId`, `listId`,
   `itemId`), so a tap on the notification can open the right screen. Today both send a `notification`
   payload only.

### Compatibility

- The new field is additive. Web and iOS keep working while they still write only `fcmToken`; with the
  fallback, the functions still reach them.
- Clients that move to `fcmTokens` stop overwriting each other.
- Security rules (not exported yet, see docs/backend.md) must allow a user to update `fcmTokens` on
  their own `users/{uid}` document, as they do for `fcmToken` today.

### Impact on Android

- **If accepted:** M6 writes `fcmTokens` with `arrayUnion` on sign-in and refresh, `arrayRemove` on
  sign-out, and also `fcmToken` until the web and iOS clients move.
- **Until it is decided:** M6 stops here, as CODEX_PLAN asks. The alternative is to build M6 now with the
  single field, exactly like the web and iOS (write on sign-in, never remove), and switch later. Android
  then takes over notifications from the user's other devices whenever they sign in on Android.

