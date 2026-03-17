## OSS -> ES 导入服务（SOFABOOT + Spring Boot 2.3.12 + ES 7.10）

### 功能点
- 从 OSS 扫描 `.dat` / `.txt` 文件并导入 Elasticsearch 7.10。
- 文件字段映射、分隔符、索引名、后缀、cron 均可配置。
- 导入日志按“任务/文件/批次”输出，便于排查。
- 支持运行时动态刷新任务配置（无需重新打包），并支持后续新增导入任务。

### 关键配置
1. 主配置（properties）: `src/main/resources/application.properties`
2. 动态任务配置: `config/import-jobs.properties`

### 动态新增任务示例
假设新增 `customer` 任务：
```properties
jobs=order,customer

job.customer.enabled=true
job.customer.cron=0 0/10 * * * ?
job.customer.bucket=your-bucket
job.customer.prefix=import/customer/
job.customer.index=customer_index
job.customer.delimiter=|
job.customer.fields=customer_id,name,level,city
job.customer.file-suffixes=.dat,.txt
job.customer.skip-header=true
job.customer.id-field=customer_id
```
保存后：
- 等待自动刷新（默认 30 秒）
- 或手动触发：`POST /admin/import-jobs/reload`

### 启动
```bash
mvn spring-boot:run
```

### 日志说明
- 任务开始/结束：包含任务名、bucket、prefix、索引、总耗时
- 文件级日志：导入条数、是否跳过（已处理）
- 批量写入日志：每 500 条一批，输出成功/失败信息

### 幂等说明
- 使用本地状态文件 `app.import.state-file` 保存已处理文件的 `ETag`。
- 同一任务下同一 object key + etag 不会重复导入。
