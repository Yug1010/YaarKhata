# YaarKhata

*Yaar* (friend) + *Khata* (ledger) - a personal finance tracker that also
handles the "who owes who" math with friends, built with Kotlin, Jetpack
Compose, and Room.

All data stays on your device - no cloud sync, no account, no ads.

## How to open this project

1. Install [Android Studio](https://developer.android.com/studio) (Koala or newer).
2. Unzip this project and open the `FinanceTracker` folder as an existing project
   (File > Open, select the folder with `settings.gradle.kts`).
3. Let Gradle sync - it will download dependencies automatically the first time.
4. Run on an emulator or physical device (min SDK 26 / Android 8.0+).

## Features

**Accounts**
- Any number of bank and cash accounts, each with its own balance
- Delete an account when you no longer need it

**Everyday money**
- Log income, expenses, and one-off adjustments (e.g. a parent sending cash,
  or a mutual fund autopay) that don't count as spending
- Categorize expenses with a search-or-create picker
- Edit any past expense - amount, account, category, or note - with account
  balances kept correct automatically

**Reports**
- Total spending, all-time or this month
- Category breakdown, tap any category to see its full transaction list
- Month-by-month spending trend
- A separate space to review every adjustment you've logged

**Friend debts**
- Track who owes you and who you owe, kept deliberately separate from
  expense tracking - splitting a bill isn't "spending"
- Editable debt entries, including switching direction (Owes me / I owe),
  with account balances corrected automatically
- Partial payments, with a running remaining balance per debt
- "Settle all" for a person in one action, with support for splitting the
  payment across multiple accounts
- One-tap WhatsApp sharing of a person's open debt summary
- Settled-up people stay visible with their full history, not hidden

**Data**
- A hidden "reset all app data" option (bottom of Reports) for starting fresh
- Uninstalling the app actually clears your data - auto-backup is disabled

## Project structure

- `data/` - Room entities (`Account`, `Category`, `Transaction`, `Person`,
  `Debt`, `DebtPayment`), DAOs, and the database class
- `repository/FinanceRepository.kt` - all the business rules: which actions
  touch account balances and which don't, category bookkeeping, debt
  settlement math, and the WhatsApp summary formatting
- `ui/` - one package per screen, plus `navigation/AppNavHost.kt` wiring them
  together with a persistent bottom nav bar for Dashboard / Accounts / Debts / Reports

## What's simplified for now

- No unit tests yet
- No input validation beyond basic amount/field checks
- No recurring transactions

These are natural next iterations whenever you want to keep building.
