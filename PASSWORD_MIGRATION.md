# 明文密码修补说明

## 背景
- 早期系统直接在 `user.password` 字段保存明文密码，`AppServlet` 通过 SQL 拼接同时校验用户名与密码，存在泄漏风险。
- 为消除明文存储，本次更新引入 `org.mindrot:jbcrypt:0.4`，并新增 `PasswordUtils` 与 `PasswordMigration`，提供兼容旧数据的迁移方案。

## 设计概览（修改部分）
| 组件 | 作用 |
| --- | --- |
| `PasswordUtils` | 封装 BCrypt 哈希、匹配以及“是否需要迁移”的判断，Cost 设为 12，可在类中集中调节。 |
| `PasswordMigration` | 连接 `db.sqlite3`，筛出所有非 BCrypt 值并批量更新为哈希；支持多次运行，已迁移条目会自动跳过。 |
| `AppServlet.authenticated` | 仅按用户名提取 `password` 字段，然后调用 `PasswordUtils.matches`。若数据库中仍存在明文，将继续兼容；一旦迁移完成，校验即走 BCrypt。 |
| Gradle 任务 `migratePasswords` | 入口命令 `./gradlew migratePasswords`，内部运行上述迁移逻辑。 |

## 迁移步骤
1. 在项目根目录启用既有环境（例如 `conda activate ibkr`）。
2. **备份数据库**：`cp db.sqlite3 db.sqlite3.bak`，必要时可用备份回滚。
3. 执行命令：
   ```bash
   cd "/Users/young/Desktop/secure computing/cw/patients2"
   ./gradlew migratePasswords
   ```
   - 控制台会输出“Password migration complete. X password(s) updated.”，若 X 为 0 表示所有条目均已是 BCrypt。
4. 验证：使用原有账号登录；若成功则表示明文密码已被无缝替换。

## 常见问题
- **重复执行**：脚本具备幂等性，多次运行只会跳过已经哈希的行。
- **新增/重置账号**：任何写入密码的代码都需调用 `PasswordUtils.hashPassword`，禁止再将明文直接插入 `user.password`。
- **回滚**：若遇异常，可停止应用、恢复 `db.sqlite3.bak` 并重新运行 `./gradlew migratePasswords`。

## 后续建议
- 在数据库层为 `user.username` 添加唯一约束，保证认证逻辑的单记录假设成立。
- 结合本次改动尽快替换 `authenticated` 中的字符串拼接 SQL，避免注入攻击。

