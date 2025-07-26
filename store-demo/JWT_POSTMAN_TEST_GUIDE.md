# JWT + Spring Security Postman 测试指南

## 测试环境配置

### 基础信息

- **服务地址**: `http://localhost:8081`
- **数据库**: MySQL `store_demo`
- **Redis**: localhost:6379 (database 0)

### 测试用户（已在 init.sql 中预置）

| 用户名  | 密码   | 角色           | 手机号      |
| ------- | ------ | -------------- | ----------- |
| admin   | 123456 | ADMIN, MANAGER | 13800138000 |
| user    | 123456 | USER           | 13800138001 |
| manager | 123456 | MANAGER        | 13800138002 |

### 注册默认角色说明

- 新注册用户默认分配角色 ID 为 1 的角色
- 根据 init.sql，角色 ID=1 对应"ADMIN"角色
- 如果需要普通用户角色，请确保数据库中角色 ID=1 对应"USER"角色

### JWT 配置（application.properties）

```properties
# PC设备配置
jwt.pc.access-token-expiration=3600000    # 1小时
jwt.pc.refresh-token-expiration=604800000  # 7天

# Mobile设备配置
jwt.mobile.access-token-expiration=7200000   # 2小时
jwt.mobile.refresh-token-expiration=1209600000 # 14天
```

## Redis Key 结构说明

### 令牌存储格式

```
access_token:{userId}:{deviceType}  -> JWT访问令牌
refresh_token:{userId}:{deviceType} -> JWT刷新令牌
```

### 示例 Redis Keys

```
access_token:1:PC      -> admin用户PC设备的访问令牌
access_token:1:MOBILE  -> admin用户Mobile设备的访问令牌
refresh_token:1:PC     -> admin用户PC设备的刷新令牌
refresh_token:1:MOBILE -> admin用户Mobile设备的刷新令牌
```

---

## 测试场景 0: 用户注册和基础验证

### 0.1 注册新用户

```http
POST http://localhost:8081/api/auth/register
Content-Type: application/x-www-form-urlencoded

username=testuser001&password=123456&phone=13800138001
```

**预期响应**:

```json
{
  "message": "注册成功"
}
```

### 0.2 验证用户注册成功

```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
    "username": "testuser001",
    "password": "123456",
    "deviceType": "PC"
}
```

**预期响应**:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 3600000,
  "deviceType": "PC",
  "tokenType": "Bearer"
}
```

**保存令牌**: 将返回的`accessToken`保存为`NEW_USER_TOKEN`，用于后续测试

### 0.3 验证新用户可以访问受保护资源

```http
GET http://localhost:8081/api/test/protected
Authorization: Bearer {NEW_USER_TOKEN}
```

**预期响应**:

```json
{
  "message": "这是一个受保护的接口，需要认证",
  "user": "testuser001",
  "roles": ["ADMIN"]
}
```

**注意**: 新注册用户默认获得 ADMIN 角色（因为默认角色 ID=1 对应 ADMIN）

### 0.4 验证新用户的管理员权限

```http
GET http://localhost:8081/api/test/admin
Authorization: Bearer {NEW_USER_TOKEN}
```

**预期响应**:

```json
{
  "message": "这是一个管理员接口，需要ADMIN角色",
  "user": "testuser001",
  "roles": ["ADMIN"]
}
```

### 0.5 获取当前用户信息

```http
GET http://localhost:8081/api/auth/me
Authorization: Bearer {NEW_USER_TOKEN}
```

**预期响应**:

```json
{
  "id": 4,
  "username": "testuser001",
  "roles": ["ADMIN"]
}
```

### 0.6 检查 Redis 中的令牌存储

```bash
redis-cli
> keys *token*
1) "access_token:4:PC"
2) "refresh_token:4:PC"

> get access_token:4:PC
"eyJhbGciOiJIUzI1NiJ9..."

> ttl access_token:4:PC
3599  # 剩余秒数
```

### 0.7 注册重复用户名测试

```http
POST http://localhost:8081/api/auth/register
Content-Type: application/x-www-form-urlencoded

username=testuser001&password=123456&phone=13800138002
```

**预期响应**:

```json
{
  "error": "用户名已存在"
}
```

### 0.8 注册第二个测试用户（用于多用户测试）

```http
POST http://localhost:8081/api/auth/register
Content-Type: application/x-www-form-urlencoded

username=testuser002&password=123456&phone=13800138002
```

**预期响应**:

```json
{
  "message": "注册成功"
}
```

### 0.9 第二个用户登录

```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
    "username": "testuser002",
    "password": "123456",
    "deviceType": "PC"
}
```

**保存令牌**: 将返回的`accessToken`保存为`SECOND_USER_TOKEN`

### 0.10 验证两个用户的令牌在 Redis 中独立存储

```bash
redis-cli
> keys *token*
1) "access_token:4:PC"    # testuser001
2) "access_token:5:PC"    # testuser002
3) "refresh_token:4:PC"   # testuser001
4) "refresh_token:5:PC"   # testuser002
```

---

## 测试场景 1: 多设备登录测试（使用注册的用户）

### 1.1 PC 设备登录

```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
    "username": "testuser001",
    "password": "123456",
    "deviceType": "PC"
}
```

**预期响应**:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 3600000,
  "deviceType": "PC",
  "tokenType": "Bearer"
}
```

**Redis 检查**:

```bash
redis-cli
> keys access_token:4:*  # 假设testuser001的ID是4
1) "access_token:4:PC"
> keys refresh_token:4:*
1) "refresh_token:4:PC"
```

### 1.2 同用户 Mobile 设备登录

```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
    "username": "testuser001",
    "password": "123456",
    "deviceType": "MOBILE"
}
```

**预期响应**:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 7200000,
  "deviceType": "MOBILE",
  "tokenType": "Bearer"
}
```

**Redis 检查** (应该看到两个设备的令牌):

```bash
> keys access_token:4:*
1) "access_token:4:PC"
2) "access_token:4:MOBILE"
> keys refresh_token:4:*
1) "refresh_token:4:PC"
2) "refresh_token:4:MOBILE"
```

### 1.3 验证两个设备都可以访问受保护资源

**PC 设备访问**:

```http
GET http://localhost:8081/api/test/protected
Authorization: Bearer {PC_ACCESS_TOKEN}
```

**Mobile 设备访问**:

```http
GET http://localhost:8081/api/test/protected
Authorization: Bearer {MOBILE_ACCESS_TOKEN}
```

**预期结果**: 两个请求都应该成功返回 200

---

## 测试场景 1: 多设备登录测试

### 1.1 PC 设备登录

```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
    "username": "admin",
    "password": "123456",
    "deviceType": "PC"
}
```

**预期响应**:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 3600000,
  "deviceType": "PC",
  "tokenType": "Bearer"
}
```

**Redis 检查**:

```bash
redis-cli
> keys access_token:1:*
1) "access_token:1:PC"
> keys refresh_token:1:*
1) "refresh_token:1:PC"
```

### 1.2 同用户 Mobile 设备登录

```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
    "username": "admin",
    "password": "123456",
    "deviceType": "MOBILE"
}
```

**预期响应**:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 7200000,
  "deviceType": "MOBILE",
  "tokenType": "Bearer"
}
```

**Redis 检查** (应该看到两个设备的令牌):

```bash
> keys access_token:1:*
1) "access_token:1:PC"
2) "access_token:1:MOBILE"
> keys refresh_token:1:*
1) "refresh_token:1:PC"
2) "refresh_token:1:MOBILE"
```

### 1.3 验证两个设备都可以访问受保护资源

**PC 设备访问**:

```http
GET http://localhost:8081/api/test/protected
Authorization: Bearer {PC_ACCESS_TOKEN}
```

**Mobile 设备访问**:

```http
GET http://localhost:8081/api/test/protected
Authorization: Bearer {MOBILE_ACCESS_TOKEN}
```

**预期结果**: 两个请求都应该成功返回 200

---

## 测试场景 2: JWT 令牌过期和自动续期

### 2.1 修改配置进行快速过期测试

**临时修改 application.properties** (仅用于测试):

```properties
# 设置很短的过期时间用于测试
jwt.pc.access-token-expiration=60000      # 1分钟
jwt.pc.refresh-token-expiration=300000    # 5分钟
jwt.mobile.access-token-expiration=120000  # 2分钟
jwt.mobile.refresh-token-expiration=600000 # 10分钟
```

### 2.2 登录获取令牌

```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
    "username": "admin",
    "password": "123456",
    "deviceType": "PC"
}
```

**保存响应中的令牌**:

- `accessToken` -> 用于后续测试
- `refreshToken` -> 用于刷新令牌

### 2.3 等待访问令牌过期 (1 分钟后)

**使用过期的访问令牌访问**:

```http
GET http://localhost:8081/api/test/protected
Authorization: Bearer {EXPIRED_ACCESS_TOKEN}
```

**预期结果**: 401 Unauthorized

### 2.4 使用刷新令牌获取新的访问令牌

```http
POST http://localhost:8081/api/auth/refresh
Content-Type: application/json

{
    "refreshToken": "{REFRESH_TOKEN}",
    "deviceType": "PC"
}
```

**预期响应**:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...", // 新的访问令牌
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...", // 相同的刷新令牌
  "expiresIn": 60000,
  "deviceType": "PC",
  "tokenType": "Bearer"
}
```

### 2.5 使用新的访问令牌访问资源

```http
GET http://localhost:8081/api/test/protected
Authorization: Bearer {NEW_ACCESS_TOKEN}
```

**预期结果**: 200 OK

---

## 测试场景 3: 单设备登出对其他设备的影响

### 3.1 确保两个设备都已登录

按照场景 1 的步骤，确保 admin 用户在 PC 和 Mobile 设备都已登录。

**Redis 检查**:

```bash
> keys *token:1:*
1) "access_token:1:PC"
2) "access_token:1:MOBILE"
3) "refresh_token:1:PC"
4) "refresh_token:1:MOBILE"
```

### 3.2 PC 设备登出

```http
POST http://localhost:8081/api/auth/logout?deviceType=PC
Authorization: Bearer {PC_ACCESS_TOKEN}
```

**预期响应**:

```json
{
  "message": "登出成功"
}
```

### 3.3 检查 Redis 中的令牌变化

```bash
> keys *token:1:*
1) "access_token:1:MOBILE"
2) "refresh_token:1:MOBILE"
```

**预期结果**: 只有 PC 设备的令牌被删除，Mobile 设备的令牌仍然存在

### 3.4 验证设备隔离效果

**PC 设备访问** (应该失败):

```http
GET http://localhost:8081/api/test/protected
Authorization: Bearer {PC_ACCESS_TOKEN}
```

**预期结果**: 401 Unauthorized

**Mobile 设备访问** (应该成功):

```http
GET http://localhost:8081/api/test/protected
Authorization: Bearer {MOBILE_ACCESS_TOKEN}
```

**预期结果**: 200 OK

---

## 测试场景 4: 同设备重复登录

### 4.1 PC 设备首次登录

```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
    "username": "admin",
    "password": "123456",
    "deviceType": "PC"
}
```

**保存第一次的令牌**: `FIRST_PC_TOKEN`

### 4.2 同设备再次登录

```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
    "username": "admin",
    "password": "123456",
    "deviceType": "PC"
}
```

**保存第二次的令牌**: `SECOND_PC_TOKEN`

### 4.3 验证旧令牌失效

```http
GET http://localhost:8081/api/test/protected
Authorization: Bearer {FIRST_PC_TOKEN}
```

**预期结果**: 401 Unauthorized (旧令牌应该失效)

### 4.4 验证新令牌有效

```http
GET http://localhost:8081/api/test/protected
Authorization: Bearer {SECOND_PC_TOKEN}
```

**预期结果**: 200 OK

---

## 测试场景 5: 不同用户多设备登录

### 5.1 admin 用户 PC 登录

```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
    "username": "admin",
    "password": "123456",
    "deviceType": "PC"
}
```

### 5.2 user 用户 PC 登录

```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
    "username": "user",
    "password": "123456",
    "deviceType": "PC"
}
```

### 5.3 admin 用户 Mobile 登录

```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
    "username": "admin",
    "password": "123456",
    "deviceType": "MOBILE"
}
```

### 5.4 检查 Redis 中的令牌分布

```bash
> keys *token:*
1) "access_token:1:PC"      # admin PC
2) "access_token:1:MOBILE"  # admin Mobile
3) "access_token:2:PC"      # user PC
4) "refresh_token:1:PC"     # admin PC
5) "refresh_token:1:MOBILE" # admin Mobile
6) "refresh_token:2:PC"     # user PC
```

---

## 测试场景 6: 角色权限测试

### 6.1 admin 用户访问管理员接口

```http
GET http://localhost:8081/api/test/admin
Authorization: Bearer {ADMIN_ACCESS_TOKEN}
```

**预期结果**: 200 OK

### 6.2 user 用户访问管理员接口

```http
GET http://localhost:8081/api/test/admin
Authorization: Bearer {USER_ACCESS_TOKEN}
```

**预期结果**: 403 Forbidden

### 6.3 user 用户访问普通用户接口

```http
GET http://localhost:8081/api/test/user
Authorization: Bearer {USER_ACCESS_TOKEN}
```

**预期结果**: 200 OK

---

## 测试场景 7: 刷新令牌过期测试

### 7.1 等待刷新令牌过期

如果你设置了很短的刷新令牌过期时间，等待其过期。

### 7.2 使用过期的刷新令牌

```http
POST http://localhost:8081/api/auth/refresh
Content-Type: application/json

{
    "refreshToken": "{EXPIRED_REFRESH_TOKEN}",
    "deviceType": "PC"
}
```

**预期结果**: 400 Bad Request，错误信息："刷新令牌无效或已过期"

---

## Redis 监控命令

### 实时监控 Redis 操作

```bash
redis-cli monitor
```

### 查看所有令牌

```bash
redis-cli
> keys *token*
```

### 查看特定用户的令牌

```bash
> keys *token:1:*  # 查看用户ID为1的所有令牌
```

### 查看令牌内容

```bash
> get access_token:1:PC
```

### 查看令牌 TTL

```bash
> ttl access_token:1:PC
```

---

## 预期测试结果总结

| 测试场景       | 预期结果                                            |
| -------------- | --------------------------------------------------- |
| 多设备登录     | ✅ 同用户可以在 PC 和 Mobile 同时登录，令牌独立存储 |
| 令牌过期续期   | ✅ 访问令牌过期后，可用刷新令牌获取新的访问令牌     |
| 单设备登出     | ✅ 单设备登出只影响该设备，其他设备不受影响         |
| 同设备重复登录 | ✅ 新登录会使旧令牌失效                             |
| 设备间隔离     | ✅ PC 和 Mobile 设备的令牌完全独立                  |
| 角色权限       | ✅ 不同角色用户访问对应权限的接口                   |
| 刷新令牌过期   | ✅ 过期的刷新令牌无法获取新的访问令牌               |

---

## 故障排除

### 常见问题

1. **401 Unauthorized**: 检查令牌是否过期或格式是否正确
2. **403 Forbidden**: 检查用户角色权限
3. **400 Bad Request**: 检查请求参数格式
4. **Redis 连接失败**: 确保 Redis 服务正常运行

### 调试技巧

1. 查看应用日志中的 JWT 相关日志
2. 使用 Redis 监控查看令牌操作
3. 检查数据库中的用户和角色数据
4. 验证 JWT 令牌的 payload 内容

---

## 测试完成后的清理

### 清理 Redis

```bash
redis-cli flushdb
```

### 重启应用

重启 Spring Boot 应用以恢复正常的令牌过期时间配置。

---

## 测试场景 X: JWT自动刷新功能测试

### X.1 准备工作 - 获取令牌对

首先登录获取Access Token和Refresh Token：

```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
    "username": "testuser001",
    "password": "123456",
    "deviceType": "PC"
}
```

**保存响应中的令牌**:
- `accessToken` -> 保存为 `ACCESS_TOKEN`
- `refreshToken` -> 保存为 `REFRESH_TOKEN`

### X.2 测试自动刷新功能

#### 方法1: 等待Access Token自然过期

如果你设置了短过期时间（如1分钟），等待Access Token过期后：

```http
GET http://localhost:8081/api/test/protected
Authorization: Bearer {EXPIRED_ACCESS_TOKEN}
X-Refresh-Token: {REFRESH_TOKEN}
```

**预期结果**:
- 状态码: 200 OK
- 响应头包含:
  - `X-New-Access-Token`: 新的访问令牌
  - `X-Token-Refreshed`: true
- 响应体: 正常的接口响应

#### 方法2: 使用无效的Access Token测试

使用一个明显无效的Access Token：

```http
GET http://localhost:8081/api/test/protected
Authorization: Bearer invalid.token.here
X-Refresh-Token: {REFRESH_TOKEN}
```

**预期结果**: 同上

### X.3 验证新Access Token有效性

从响应头中获取新的Access Token，然后测试：

```http
GET http://localhost:8081/api/test/protected
Authorization: Bearer {NEW_ACCESS_TOKEN}
```

**预期结果**: 200 OK，正常访问

### X.4 测试没有Refresh Token的情况

只发送过期的Access Token，不发送Refresh Token：

```http
GET http://localhost:8081/api/test/protected
Authorization: Bearer {EXPIRED_ACCESS_TOKEN}
```

**预期结果**: 401 Unauthorized（无法自动刷新）

### X.5 测试Refresh Token也过期的情况

使用过期的Refresh Token：

```http
GET http://localhost:8081/api/test/protected
Authorization: Bearer {EXPIRED_ACCESS_TOKEN}
X-Refresh-Token: {EXPIRED_REFRESH_TOKEN}
```

**预期结果**: 401 Unauthorized（无法自动刷新）

### X.6 Redis检查 - 验证令牌更新

自动刷新后，检查Redis中的令牌：

```bash
redis-cli
> get access_token:4:PC
# 应该看到新的Access Token

> ttl access_token:4:PC
# 应该看到重新设置的TTL时间
```

---

## 自动刷新 vs 手动刷新对比

| 特性 | 手动刷新 | 自动刷新 |
|------|----------|----------|
| **触发方式** | 客户端主动调用 `/api/auth/refresh` | 服务端检测到Access Token过期时自动触发 |
| **客户端实现** | 需要处理401响应，调用刷新接口 | 只需在请求头中包含Refresh Token |
| **网络请求** | 需要额外的刷新请求 | 无需额外请求，在原请求中完成 |
| **用户体验** | 可能出现短暂的认证失败 | 对用户透明，无感知刷新 |
| **实现复杂度** | 客户端需要实现刷新逻辑 | 服务端自动处理，客户端简单 |

---

## 前端JavaScript自动刷新示例

### 方案1: 使用自动刷新过滤器

```javascript
// 封装的API请求函数
async function apiRequest(url, options = {}) {
    const accessToken = localStorage.getItem('accessToken');
    const refreshToken = localStorage.getItem('refreshToken');
    
    const response = await fetch(url, {
        ...options,
        headers: {
            'Authorization': `Bearer ${accessToken}`,
            'X-Refresh-Token': refreshToken,
            'Content-Type': 'application/json',
            ...options.headers
        }
    });
    
    // 检查是否有新的Access Token
    const newAccessToken = response.headers.get('X-New-Access-Token');
    const tokenRefreshed = response.headers.get('X-Token-Refreshed');
    
    if (tokenRefreshed === 'true' && newAccessToken) {
        console.log('令牌已自动刷新');
        localStorage.setItem('accessToken', newAccessToken);
    }
    
    return response;
}

// 使用示例
apiRequest('/api/test/protected')
    .then(response => response.json())
    .then(data => console.log(data))
    .catch(error => console.error('请求失败:', error));
```

### 方案2: 传统手动刷新

```javascript
async function apiRequestWithManualRefresh(url, options = {}) {
    let accessToken = localStorage.getItem('accessToken');
    
    let response = await fetch(url, {
        ...options,
        headers: {
            'Authorization': `Bearer ${accessToken}`,
            'Content-Type': 'application/json',
            ...options.headers
        }
    });
    
    // 如果401，尝试刷新令牌
    if (response.status === 401) {
        const refreshed = await refreshAccessToken();
        if (refreshed) {
            // 重新发送原始请求
            accessToken = localStorage.getItem('accessToken');
            response = await fetch(url, {
                ...options,
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json',
                    ...options.headers
                }
            });
        } else {
            // 刷新失败，跳转到登录页
            window.location.href = '/login';
            return;
        }
    }
    
    return response;
}

async function refreshAccessToken() {
    const refreshToken = localStorage.getItem('refreshToken');
    const deviceType = localStorage.getItem('deviceType') || 'PC';
    
    try {
        const response = await fetch('/api/auth/refresh', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                refreshToken: refreshToken,
                deviceType: deviceType
            })
        });
        
        if (response.ok) {
            const data = await response.json();
            localStorage.setItem('accessToken', data.accessToken);
            return true;
        }
    } catch (error) {
        console.error('刷新令牌失败:', error);
    }
    
    return false;
}
```

---

## 推荐使用方式

### 生产环境推荐
- **自动刷新**: 用于提升用户体验，减少认证中断
- **手动刷新**: 作为备用方案，处理自动刷新失败的情况

### 安全考虑
1. **Refresh Token存储**: 建议存储在HttpOnly Cookie中，而不是localStorage
2. **令牌轮换**: 考虑在刷新时同时更新Refresh Token
3. **设备绑定**: Refresh Token应该与特定设备绑定
4. **异常监控**: 监控异常的刷新行为，防止令牌被滥用

---

## 故障排除

### 自动刷新不工作
1. 检查请求头是否包含 `X-Refresh-Token` 或 `Refresh-Token`
2. 验证Refresh Token是否有效且未过期
3. 确认AutoRefreshJwtFilter已正确配置
4. 检查Redis中的Refresh Token是否存在

### 手动刷新失败
1. 验证刷新接口 `/api/auth/refresh` 是否可访问
2. 检查请求格式是否正确（JSON格式）
3. 确认deviceType参数是否匹配
4. 验证Refresh Token是否在Redis中存在且有效