# Attack Tree: Unauthorized Access via Plaintext Passwords

```mermaid
graph TD
  R["Root: Unauthorized Access to Sensitive Patient Records"]

  R -->|"AND"| C1["Compromise Credentials"]

  C1 -->|"OR"| M1["Steal Database / Backups"]
  M1 -->|"AND"| S1["Obtain `db.sqlite3` / backup archive"]
  S1 --- EVID1["Evidence: database stored beside app, no extra protection"]
  M1 -->|"AND"| S2["Extract plaintext `user.password` values"]
  S2 --- EVID2["Evidence: `sqlite3 db.sqlite3 \"select username, password from user;\"` shows entries like `plain_demo|secret123`"]

  C1 -->|"OR"| M2["Intercept credentials in transit (fallback path)"]
  %% focus on plaintext storage path

  R -->|"AND"| C2["Reuse stolen credentials to log in"]
  C2 -->|"AND"| L1["Submit valid username/password via login form"]
  L1 --- EVID3["Evidence: `AppServlet` authenticates with `PasswordUtils.matches` once plaintext retrieved"]
  C2 -->|"AND"| L2["Query surnames to enumerate patient records"]

  R -->|"OR"| IMP1["Impact: disclosure of patient names, DOB, addresses, diagnoses"]
  R -->|"OR"| IMP2["Impact: downstream account takeover if passwords reused elsewhere"]
```

**说明**
- **AND** 边表示必须同时满足的步骤；**OR** 边表示可任选其一（这里只展开了“窃取数据库→明文密码”路径，保留其它可能分支以示完整性）。
- 节点 `S2` 体现了漏洞本身：数据库直接保存明文密码，攻击者只需离线执行一条 `sqlite3` 查询即可获得所有凭证。
- 节点 `C2` 展示利用阶段：有了合法凭证即可通过登录表单访问医生名下的全部患者，再逐个姓氏枚举实现“未授权访问”。

