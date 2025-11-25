# Password Migration Guide

## Background
- The original system stored plaintext passwords directly in the `user.password` field, with `AppServlet` using SQL string concatenation to verify both username and password, creating a credential exposure risk.
- To eliminate plaintext storage, this update introduces `org.mindrot:jbcrypt:0.4` and adds `PasswordUtils` and `PasswordMigration`, providing a migration solution compatible with legacy data.

## Design Overview (Modified Components)
| Component | Purpose |
| --- | --- |
| `PasswordUtils` | Encapsulates BCrypt hashing, matching, and "requires migration" checks. Cost factor set to 12, adjustable centrally within the class. |
| `PasswordMigration` | Connects to `db.sqlite3`, identifies all non-BCrypt values, and batch-updates them to hashes. Supports multiple runs; already-migrated entries are automatically skipped. |
| `AppServlet.authenticated` | Queries only by username to retrieve the `password` field, then calls `PasswordUtils.matches`. If plaintext remains in the database, it continues to work; once migration completes, verification uses BCrypt. |
| Gradle task `migratePasswords` | Entry command `./gradlew migratePasswords`, executes the migration logic above. |

## Migration Steps
1. Activate your existing environment in the project root (e.g., `conda activate ibkr`).
2. **Backup the database**: `cp db.sqlite3 db.sqlite3.bak` for rollback if needed.
3. Execute the command:
   ```bash
   cd "/Users/young/Desktop/secure computing/cw/patients2"
   ./gradlew migratePasswords
   ```
   - Console output will show "Password migration complete. X password(s) updated." If X is 0, all entries are already BCrypt hashes.
4. Verify: Log in with existing accounts; success indicates plaintext passwords have been seamlessly replaced.

## FAQ
- **Repeated execution**: The script is idempotent; multiple runs will skip already-hashed rows.
- **Adding/resetting accounts**: Any code writing passwords must call `PasswordUtils.hashPassword`; never insert plaintext directly into `user.password`.
- **Rollback**: If issues occur, stop the application, restore `db.sqlite3.bak`, and re-run `./gradlew migratePasswords`.

## Future Recommendations
- Add a unique constraint on `user.username` at the database level to ensure the single-record assumption in authentication logic holds.
- Replace SQL string concatenation in `authenticated` to prevent injection attacks, building on this update.

