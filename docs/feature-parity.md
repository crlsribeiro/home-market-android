# Feature parity with the iOS app

One row per iOS screen and feature, the milestone from `CODEX_PLAN.md` that covers it on Android, and its
status. Update the status column in every pull request.

Status values: `todo`, `in progress`, `done`, `out of scope` (decided by the repository owner not to build on
Android). Business rules are the same as in the existing apps: Android keeps the iOS rule for every
feature, except where `backend.md` records a decision to follow the web.

The iOS source file is given for each screen so the behaviour can be checked. Backend details are in
[`backend.md`](backend.md).

## App shell and navigation

| iOS screen / feature | iOS source | Milestone | Status |
|---|---|---|---|
| Loading screen while the auth state resolves | `Views/Common/RootView.swift` | M1 | done |
| Routing: signed out → Login; signed in without household → Onboarding; otherwise main tabs | `Views/Common/RootView.swift` | M1, M2 | done |
| Main tabs: List, Admin (admin only), History, Account | `Views/Common/RootView.swift` | M3 (List), M2 (Admin), M7 (History, admin only on Android), M8 (Account) | in progress (List, Admin and a basic Account tab done; History in M7) |
| Full-screen takeover while the list status is `shopping`: Shopping mode for the admin, waiting screen for members | `Views/Common/RootView.swift` | M5 | todo |
| Brand design system (colors, buttons, cards, badges) | `Views/Common/DesignSystem.swift` | M0 (Material 3 theme), later milestones (components) | in progress |

## Authentication

| iOS screen / feature | iOS source | Milestone | Status |
|---|---|---|---|
| Login screen: email + password | `Views/Login/LoginView.swift` | M1 | done |
| Sign in with Google | `Views/Login/LoginView.swift`, `Services/AuthService.swift` | M1 | done (Credential Manager) |
| Sign in with Apple | `Views/Login/LoginView.swift`, `Services/AuthService.swift` | — | out of scope |
| Forgot password sheet: send reset email, confirmation state | `Views/Login/ForgotPasswordView.swift` | M8 | todo |
| Register screen: first name, last name, email, password, confirm password | `Views/Login/RegisterView.swift` | M1 | done |
| Register: validation (names and email required, password ≥ 8 characters, passwords match, complete phone or empty) | `Views/Login/RegisterView.swift` | M1 | done |
| Register: phone number with country picker and input mask (BR, US, PT, AR, ES) | `Views/Login/RegisterView.swift`, `Models/PhoneCountry.swift` | M8 | todo |
| Register: optional profile photo from camera or gallery, uploaded to `users/{uid}/avatar` | `Views/Login/RegisterView.swift` | M8 | todo |
| Register: links to Terms of Use and Privacy Policy | `Views/Login/RegisterView.swift` | M8 | todo |
| Create `users/{uid}` on first sign-in; keep the session across restarts | `Services/AuthService.swift`, `ViewModels/AuthViewModel.swift` | M1 | done |
| Sign out with confirmation dialog | `Views/MainList/MainListView.swift` | M1 | done |

## Household

| iOS screen / feature | iOS source | Milestone | Status |
|---|---|---|---|
| Onboarding: choose "create household" or "join with invite code" | `Views/Common/OnboardingView.swift` | M2 | done |
| Create household with a name; creator becomes admin | `Views/Common/OnboardingView.swift`, `Services/HouseholdService.swift` | M2 | done |
| Join household with an invite code; "invalid code" error | `Views/Common/OnboardingView.swift`, `ViewModels/HouseholdViewModel.swift` | M2 | done |
| Admin panel: household name and member list with admin badge | `Views/Admin/AdminPanelView.swift` | M2 | done |
| Admin panel: "you're the only one here" hint | `Views/Admin/AdminPanelView.swift` | M2 | done |
| Admin panel: show invite code, copy it to the clipboard and share it with the share sheet | `Views/Admin/AdminPanelView.swift` | M2 | done |
| Admin panel: generate a new invite code | `Views/Admin/AdminPanelView.swift` | M2 | done |

## Weekly list

| iOS screen / feature | iOS source | Milestone | Status |
|---|---|---|---|
| Header: greeting with the user's name and avatar, list status badge, week label | `Views/MainList/MainListView.swift` | M3 | done (initials avatar; the profile photo comes with M8) |
| Summary cards: total, purchased, pending, urgent | `Views/MainList/MainListView.swift` | M3 | done |
| Current list items with thumbnail, urgent badge, quantity, author and status badge (pending, purchased, not found, awaiting approval) | `Views/MainList/MainListView.swift` | M3, M4 (awaiting approval) | done |
| Empty list message | `Views/MainList/MainListView.swift` | M3 | done |
| "Next week" section with `rolled_over` items | `Views/MainList/MainListView.swift` | M3 | done |
| No active list: admin can create this week's list; members see "wait for the admin" | `Views/MainList/MainListView.swift` | M3 | done |
| Swipe to delete an item | `Views/MainList/MainListView.swift` | M3 | todo |
| Add item sheet: name, quantity stepper (1–99), notes, urgent toggle | `Views/MainList/MainListView.swift` (`AddItemView`) | M3 | todo |
| Add item: optional photo from camera or gallery, uploaded to Storage | `Views/MainList/MainListView.swift`, `Views/Common/CameraPicker.swift` | M3 | todo |
| Adding an item with no active list creates this week's list first | `ViewModels/ListViewModel.swift` | M3 | todo |
| Item detail: photo, name, notes, quantity, requested by, time added, week | `Views/MainList/ItemDetailView.swift` | M3 | todo |
| Item detail: edit notes | `Views/MainList/ItemDetailView.swift` | M3 | todo |
| Item detail: add or replace the photo after creation | `Views/MainList/ItemDetailView.swift`, `Services/ListService.swift` | M3 | todo |
| Item detail: remove from list with a confirmation step (hidden when the list is closed) | `Views/MainList/ItemDetailView.swift` | M3 | todo |
| Item detail: price per unit and total, admin only ("available after receipt upload" otherwise) | `Views/MainList/ItemDetailView.swift` | M7 | todo |
| Item detail: "mark as purchased" while shopping | `Views/MainList/ItemDetailView.swift` | M5 | todo |
| Error message banner for failed writes | `Views/MainList/MainListView.swift` | M3 | in progress (snackbar for creating the list) |
| Automatic close of a list whose week has ended (weekly cut, no purchase), then a new list for this week | `ViewModels/ListViewModel.swift` (`expireIfStale`) | M4 | todo |

## Lifecycle and approvals

| iOS screen / feature | iOS source | Milestone | Status |
|---|---|---|---|
| Admin panel: list status card (week, status) | `Views/Admin/AdminPanelView.swift` | M4 | todo |
| Items added after the list leaves `open` get `approvalStatus: "pending"` | `Services/ListService.swift` | M4 | todo |
| Admin panel: pending approvals with approve and reject | `Views/Admin/AdminPanelView.swift` | M4 | todo |
| Start shopping from the list (cart button, admin only, list `open` or `locked`, not empty) | `Views/MainList/MainListView.swift` | M5 | todo |
| Lock and reopen the list (web feature; iOS never writes `locked`) | — | M4 | todo |
| Admin panel: "items per person this week" bar chart with expandable item names | `Views/Admin/AdminPanelView.swift` (`WeeklyDashboardSection`) | M4 | todo |

## Shopping mode

| iOS screen / feature | iOS source | Milestone | Status |
|---|---|---|---|
| Progress card: week, "x of y items", progress bar | `Views/Shopping/ShoppingModeView.swift` | M5 | todo |
| "To get" list with "got it" and "not available" actions. Android follows the web: "not available" writes `not_found` | `Views/Shopping/ShoppingModeView.swift` | M5 | todo |
| "Not found" section ("notification sent") | `Views/Shopping/ShoppingModeView.swift` | M5 | todo |
| The member who added a not-found item resolves it (web flow; iOS has none) | — (web `NotFoundModal.tsx`) | M5 | todo |
| "Already picked up" section | `Views/Shopping/ShoppingModeView.swift` | M5 | todo |
| Close list: weekly cut, then create the purchase record | `Views/Shopping/ShoppingModeView.swift` | M5 (cut), M7 (purchase record) | todo |
| Abandon shopping with confirmation (list back to `open`) | `Views/Shopping/ShoppingModeView.swift` | M5 | todo |
| Members' waiting screen: week, what is happening, notification hint | `Views/Shopping/ShoppingWaitingView.swift` | M5 | todo |

## Push notifications

| iOS screen / feature | iOS source | Milestone | Status |
|---|---|---|---|
| Ask for notification permission after sign-in and save the FCM token to `users/{uid}.fcmToken` | `Services/PushNotificationService.swift` | M6 | todo |
| Receive "item added" and "item not found" notifications | `functions/src/index.ts` (web repo) | M6 | todo |

## History

| iOS screen / feature | iOS source | Milestone | Status |
|---|---|---|---|
| Purchase history list: week label, store name, total; empty state. Admin only on Android (web behaviour) | `Views/History/HistoryView.swift` | M7 | todo |
| Purchase detail: total and line items (quantity, unit price, line total) | `Views/History/HistoryView.swift` | M7 | todo |
| Upload or re-upload a receipt photo; on-device OCR and parsing replace the line items | `Views/History/HistoryView.swift`, `Services/HistoryService.swift` | M7 | todo |
| Store name extracted from the receipt | `Services/HistoryService.swift` | M7 | todo |
| Edit a line item's name and unit price; total recomputed | `Views/History/HistoryView.swift` (`EditPurchaseItemView`) | M7 | todo |

## Account

| iOS screen / feature | iOS source | Milestone | Status |
|---|---|---|---|
| Account tab: profile photo, name, email | `Views/Common/AccountSettingsView.swift` | M8 | todo |
| Change profile photo from camera or gallery | `Views/Common/AccountSettingsView.swift` | M8 | todo |
| Edit phone number with country picker | `Views/Common/AccountSettingsView.swift` | M8 | todo |
| Change login email (verification link sent to the new address) | `Views/Common/AccountSettingsView.swift`, `Services/AuthService.swift` | M8 | todo |
| Privacy Policy link (in-app browser) | `Views/Common/AccountSettingsView.swift`, `Views/Common/SafariView.swift` | M8 | todo |
| Delete account with confirmation; admin hand-off to another member; re-authentication error | `Views/Common/DeleteAccountView.swift`, `ViewModels/HouseholdViewModel.swift` | M8 | todo |

## Scope decisions

Decided by the repository owner on 2026-10-03:

- **Sign in with Apple is out of scope on Android.** Android users sign in with email/password or Google.
  Accounts created with Apple on iOS keep working on iOS. They can only sign in on Android if the same
  Firebase account also has a password or Google sign-in.
- **The business rules are the same as in the existing apps.** Every iOS feature that had no milestone now
  has one and keeps the iOS rule: the list created when the first item is added (M3), replacing an item
  photo (M3), the automatic close of last week's list (M4), the items-per-person chart (M4), starting
  shopping from the list and abandoning shopping (M5), and copying the invite code (M2). The lock step from
  the web stays in M4, next to the iOS shortcut.
- Forgot password, the registration phone and photo, the Terms and Privacy links, account settings and
  account deletion are in milestone M8 (Account). Release readiness is M9.
