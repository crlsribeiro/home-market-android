# Backend proposals

Backend changes the Android app needs or has found, written here instead of implemented, as `AGENTS.md`
requires. The Android repository never changes Cloud Functions, security rules or the data model. The
repository owner decides and makes each change in the web repository (`crlsribeiro/home-market-app`).

| # | Title | Status |
|---|---|---|
| 1 | `onItemAdded` reads a collection that no client writes | Accepted; the owner fixes it in the web repository |

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

### Related limitation (not proposed yet)

`users/{uid}.fcmToken` holds one token, so only the last device a user signed in on gets notifications.
M6 decides whether this needs a proposal and, if it does, adds it here as proposal 2.
