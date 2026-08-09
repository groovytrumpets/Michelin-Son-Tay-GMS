# Quản lý thay đổi database bằng Liquibase

Từ nay **không chạy SQL bằng tay lên database nữa**. Mọi thay đổi schema đều được
viết thành changeset, commit cùng code, và tự động áp dụng khi ứng dụng khởi động.

## Nguyên tắc

| | |
|---|---|
| Công cụ | Liquibase (YAML), chạy tự động lúc Spring Boot khởi động |
| Nơi khai báo | `src/main/resources/db/changelog/` |
| Bảng theo dõi | `DATABASECHANGELOG` (lịch sử) và `DATABASECHANGELOGLOCK` (khoá chống chạy song song) — Liquibase tự tạo |
| Hibernate | `ddl-auto=none`, **không được** đổi thành `update`. Schema chỉ do Liquibase quản lý |

Database hiện tại được coi là **mốc khởi đầu**: Liquibase chỉ quản lý các thay đổi
từ nay về sau, không mô tả lại ~90 bảng đã có.

## Cấu trúc thư mục

```
src/main/resources/db/changelog/
├── db.changelog-master.yaml        # danh sách include, Liquibase đọc file này
└── changes/
    ├── 001-booking-preassign.yaml
    └── 002-customer-company-fields.yaml
```

## Thêm một thay đổi mới

1. Tạo file `changes/00X-mo-ta-ngan.yaml` (số thứ tự tăng dần).
2. Khai báo thêm một dòng `include` ở **cuối** `db.changelog-master.yaml`.
3. Khởi động ứng dụng — Liquibase tự áp dụng.

Ví dụ thêm một cột:

```yaml
databaseChangeLog:
  - changeSet:
      id: 003-1-them-cot-vi-du
      author: ten-ban
      comment: Mô tả ngắn gọn thay đổi
      preConditions:
        - onFail: MARK_RAN
        - not:
            - columnExists:
                tableName: ten_bang
                columnName: ten_cot
      changes:
        - addColumn:
            tableName: ten_bang
            columns:
              - column:
                  name: ten_cot
                  type: VARCHAR(255)
                  remarks: Giải thích cột này dùng làm gì
      rollback:
        - dropColumn:
            tableName: ten_bang
            columnName: ten_cot
```

### Ba quy tắc bắt buộc

1. **`id` phải là duy nhất** trong toàn bộ dự án. Đặt theo `<số file>-<số thứ tự>-<mô tả>`.
2. **Không bao giờ sửa changeset đã chạy.** Liquibase lưu checksum của từng
   changeset; sửa file cũ sẽ khiến ứng dụng không khởi động được với lỗi
   `Validation Failed: checksum changed`. Cần đổi thì viết changeset **mới**.
3. **Luôn viết `rollback`.** Liquibase tự suy ra được rollback cho `addColumn`,
   `createTable`, `createIndex`… nhưng với `sql`, `insert`, `update` thì không —
   phải tự viết, nếu không lệnh rollback sẽ dừng giữa chừng.

### `preConditions` dùng để làm gì

`onFail: MARK_RAN` cho phép changeset **tự nhận biết đã được áp dụng hay chưa**.
Cần thiết vì các database hiện tại (máy dev, staging, production) đang ở trạng
thái khác nhau do trước đây chạy SQL tay. Cột đã tồn tại → Liquibase ghi nhận là
đã chạy và đi tiếp, thay vì báo lỗi và chặn khởi động.

Với changeset mới hoàn toàn thì `preConditions` không bắt buộc, nhưng giữ thói
quen này vẫn an toàn hơn.

## Rollback

Ứng dụng **không** tự rollback. Đây là thao tác thủ công, chạy bằng Maven plugin.

Chuẩn bị một lần:

```bash
cp liquibase.properties.example liquibase.properties
# điền url / username / password thật (file này đã được .gitignore)
```

Các lệnh hay dùng:

```bash
# Xem changeset nào chưa chạy
./mvnw liquibase:status

# Lùi lại N changeset gần nhất
./mvnw liquibase:rollback -Dliquibase.rollbackCount=1

# Lùi về thời điểm cụ thể
./mvnw liquibase:rollback -Dliquibase.rollbackToDate=2026-08-08

# Xem trước câu SQL sẽ chạy mà không thực thi
./mvnw liquibase:updateSQL
./mvnw liquibase:rollbackSQL -Dliquibase.rollbackCount=1
```

> Rollback trên production luôn phải backup trước. `dropColumn` **xoá dữ liệu
> vĩnh viễn**, Liquibase không khôi phục lại được nội dung cột.

## Thay đổi lớn hoặc dữ liệu seed

Với những thứ khó diễn đạt bằng YAML (thủ tục, seed hàng trăm dòng), dùng
changeset kiểu `sqlFile` và **tự viết rollback**:

```yaml
  - changeSet:
      id: 00X-1-seed-du-lieu
      author: ten-ban
      changes:
        - sqlFile:
            path: changes/sql/00X-seed.sql
            relativeToChangelogFile: true
            splitStatements: true
      rollback:
        - delete:
            tableName: ten_bang
```

## Lưu ý khi triển khai nhiều người

- Hai người cùng thêm changeset thì đánh số khác nhau, merge xong kiểm tra lại
  thứ tự `include` trong master changelog.
- Nếu ứng dụng bị tắt đột ngột giữa lúc migrate, bảng `DATABASECHANGELOGLOCK` có
  thể kẹt khoá. Gỡ bằng `./mvnw liquibase:releaseLocks`.
- CI/CD chỉ cần chạy ứng dụng là schema tự đồng bộ, không cần bước import SQL riêng.

## Các file SQL cũ

`docs/legacy-manual-migrations/` lưu lại những file SQL chạy tay trước khi có
Liquibase, chỉ để tra cứu lịch sử. **Không chạy lại** — nội dung của chúng đã
được chuyển thành changeset tương ứng.
