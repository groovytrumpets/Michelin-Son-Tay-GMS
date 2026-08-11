# Thiết kế chức năng Tin tức / Blog

Tài liệu thiết kế cho phân hệ **bài viết tin tức tuỳ chỉnh** của Michelin Sơn Tây:
soạn bài, duyệt bài, hẹn giờ đăng, và — quan trọng nhất — **cơ chế link bài viết
để câu view** (chia sẻ Facebook/Zalo ra ảnh + tiêu đề, Google index được).

> Schema do **Liquibase** quản lý (`db/changelog/changes/006-news-blog.yaml`).
> Không chạy SQL tay. Xem `docs/DATABASE_MIGRATION.md`.

---

## 1. Vì sao phải làm mới, không dùng bảng `service` sẵn có

Hệ thống đã có "blog" nhưng nó **gắn chặt vào một mặt hàng**: bảng `service` +
`service_media`, luôn phải có `catalog_item` cha, dùng để mô tả dịch vụ/phụ tùng.
Nó **không có** `slug`, tác giả, danh mục, tag, lượt xem, lịch đăng, trường SEO.

Tin tức là thực thể độc lập (bài kiến thức, tin khuyến mãi, tin garage) nên tách
bộ bảng `post_*` riêng, và **liên kết mềm** sang `catalog_item` qua khối CTA
(`post_cta`) khi bài cần chốt đơn.

---

## 2. Mô hình dữ liệu

```
post_category 1 ──< post >── N post_tag        (qua post_tag_map)
                     │
                     ├──< post_cta          → catalog_item (nullable)
                     ├──< post_view_log     (đo view + nguồn UTM)
                     └──< post_slug_history (giữ link cũ, 301 khi đổi slug)
```

### 2.1 `post_category` — danh mục tin

| Cột | Kiểu | Ghi chú |
|---|---|---|
| `category_id` | INT PK AI | |
| `name` | VARCHAR(120) NOT NULL | "Khuyến mãi", "Kiến thức xe"… |
| `slug` | VARCHAR(160) UNIQUE NOT NULL | dùng trong URL |
| `description` | VARCHAR(500) | |
| `display_order` | INT default 0 | thứ tự hiển thị |
| `is_active` | TINYINT(1) default 1 | |
| `seo_title`, `seo_description` | VARCHAR(200)/(320) | meta cho trang danh mục |
| `created_at`, `updated_at` | DATETIME(6) | |

### 2.2 `post` — bài viết

| Cột | Kiểu | Ghi chú |
|---|---|---|
| `post_id` | BIGINT PK AI | |
| `slug` | VARCHAR(200) UNIQUE NOT NULL | **khoá của URL**, không chứa id |
| `title` | VARCHAR(250) NOT NULL | |
| `excerpt` | VARCHAR(500) | tóm tắt, fallback cho `og:description` |
| `content_html` | LONGTEXT | HTML đã làm sạch phía server |
| `thumbnail_url` | VARCHAR(500) | ảnh card ở trang danh sách |
| `cover_url` | VARCHAR(500) | ảnh lớn đầu bài |
| `category_id` | INT FK → `post_category` | nullable |
| `status` | VARCHAR(20) | `DRAFT`/`PENDING`/`SCHEDULED`/`PUBLISHED`/`ARCHIVED` |
| `is_featured` | TINYINT(1) | bài nổi bật (hero trang tin) |
| `pinned_order` | INT NULL | có giá trị = ghim, sắp tăng dần |
| `published_at` | DATETIME(6) NULL | thời điểm đăng thực tế |
| `scheduled_at` | DATETIME(6) NULL | hẹn giờ; job đăng khi tới hạn |
| `author_staff_id` | INT FK → `staff_profile` | |
| `reviewer_staff_id` | INT FK → `staff_profile` NULL | người duyệt |
| `review_note` | VARCHAR(500) | lý do trả lại bài |
| `view_count` | BIGINT default 0 | bộ đếm phi chuẩn hoá, đọc nhanh |
| `share_count` | BIGINT default 0 | |
| `reading_minutes` | INT | tính từ số từ lúc lưu |
| `seo_title` | VARCHAR(200) | rỗng thì lấy `title` |
| `seo_description` | VARCHAR(320) | rỗng thì lấy `excerpt` |
| `seo_keywords` | VARCHAR(500) | |
| `og_image_url` | VARCHAR(500) | rỗng thì lấy `cover_url`/`thumbnail_url` |
| `canonical_url` | VARCHAR(500) | khi bài đăng lại từ nguồn khác |
| `allow_index` | TINYINT(1) default 1 | `noindex` cho bài nội bộ |
| `created_at`, `updated_at` | DATETIME(6) | |
| `deleted_at` | DATETIME(6) NULL | xoá mềm |

Index: `slug` UNIQUE, `(status, published_at)`, `category_id`, `deleted_at`.

### 2.3 `post_tag` / `post_tag_map`

`post_tag`: `tag_id` INT PK, `name` VARCHAR(100), `slug` VARCHAR(120) UNIQUE,
`usage_count` INT, `created_at`.
`post_tag_map`: PK kép `(post_id, tag_id)`, FK cả hai chiều, `ON DELETE CASCADE`.

### 2.4 `post_cta` — khối chốt đơn chèn trong bài

| Cột | Kiểu | Ghi chú |
|---|---|---|
| `cta_id` | BIGINT PK AI | |
| `post_id` | BIGINT FK | |
| `cta_type` | VARCHAR(20) | `BOOKING`/`CATALOG_ITEM`/`COMBO`/`PHONE`/`EXTERNAL` |
| `label` | VARCHAR(150) | "Đặt lịch thay lốp" |
| `sub_label` | VARCHAR(250) | dòng phụ |
| `target_url` | VARCHAR(500) | dùng cho `EXTERNAL` |
| `catalog_item_id` | INT FK → `catalog_item` NULL | trỏ thẳng sản phẩm/dịch vụ |
| `position` | VARCHAR(20) | `TOP`/`MIDDLE`/`BOTTOM` |
| `display_order` | INT | |
| `is_active` | TINYINT(1) | |

### 2.5 `post_view_log` — đo view và nguồn traffic

`view_id` BIGINT PK, `post_id` FK, `visitor_hash` VARCHAR(64) (SHA-256 của
IP + User-Agent + muối), `session_key` VARCHAR(64), `viewed_at` DATETIME(6),
`referrer` VARCHAR(500), `utm_source`/`utm_medium`/`utm_campaign`/`utm_content`
VARCHAR(120), `device` VARCHAR(20).
Index `(post_id, visitor_hash, viewed_at)` để chống đếm trùng.

### 2.6 `post_slug_history` — giữ link cũ

`history_id` BIGINT PK, `post_id` FK, `old_slug` VARCHAR(200) UNIQUE,
`created_at`. Khi biên tập đổi slug, slug cũ vẫn `301` về slug mới → không mất
thứ hạng Google và không chết link đã chia sẻ.

---

## 3. Cơ chế link bài viết để câu view

Đây là phần quyết định. Hiện tại FE là **SPA Vite thuần, không SSR**: bot
Facebook/Zalo tải HTML về chỉ thấy `<div id="root">` rỗng → chia sẻ link ra thẻ
xám không tiêu đề, không ảnh. Google có chạy JS nhưng chậm và tốn crawl budget.

### 3.1 Cấu trúc URL

| Trang | URL |
|---|---|
| Danh sách tin | `/tin-tuc` |
| Chi tiết bài | `/tin-tuc/{slug}` |
| Theo danh mục | `/tin-tuc/danh-muc/{categorySlug}` |
| Theo tag | `/tin-tuc/tag/{tagSlug}` |

`slug` sinh từ tiêu đề: bỏ dấu tiếng Việt, hạ chữ thường, thay ký tự lạ bằng `-`,
cắt 200 ký tự. Trùng thì nối `-2`, `-3`… **Không nhét id vào URL** — URL sạch,
dễ đọc, dễ chia sẻ; tính duy nhất do cột UNIQUE bảo đảm.

### 3.2 Prerender cho bot (giải pháp đã chọn)

Backend mở endpoint trả **HTML tĩnh đầy đủ thẻ meta**:

```
GET /seo/prerender/tin-tuc/{slug}
```

Nginx nhận diện User-Agent bot rồi chuyển hướng nội bộ, người dùng thật vẫn nhận
SPA như cũ — không phải đụng vào kiến trúc FE:

```nginx
map $http_user_agent $mst_is_crawler {
    default 0;
    "~*facebookexternalhit|Facebot|Twitterbot|LinkedInBot|Slackbot|TelegramBot|WhatsApp|Zalo|zalo|coccocbot|Googlebot|bingbot|Discordbot|Pinterest" 1;
}

server {
    location ^~ /tin-tuc/ {
        if ($mst_is_crawler) {
            proxy_pass https://api.sontaygarage.vn/seo/prerender$uri;
        }
        try_files $uri /index.html;
    }
}
```

Bản đầy đủ, dán được ngay: `docs/nginx-news-seo.conf` ở **repo frontend**.

HTML prerender chứa: `<title>`, `meta description`, `canonical`,
`og:type=article`, `og:title`, `og:description`, `og:image` (ảnh tuyệt đối),
`og:url`, `article:published_time`, `twitter:card=summary_large_image`, JSON-LD
`NewsArticle` + `BreadcrumbList`, và **cả nội dung bài dạng HTML** để Google đọc
được ngay không cần chạy JS.

### 3.3 Sitemap, RSS, robots

| Endpoint | Nội dung |
|---|---|
| `GET /seo/sitemap.xml` | sitemap index |
| `GET /seo/sitemap-posts.xml` | mọi bài `PUBLISHED` + `allow_index=1`, kèm `lastmod` |
| `GET /seo/news-sitemap.xml` | bài trong 48h, định dạng Google News |
| `GET /seo/rss.xml` | 30 bài mới nhất |
| `GET /seo/robots.txt` | trỏ tới sitemap |

`public/robots.txt` của FE trỏ sitemap về domain API.

### 3.4 Link chia sẻ kèm UTM

Nút chia sẻ ở cuối bài sinh link có `utm_source` theo kênh
(`facebook`/`zalo`/`copy`), `utm_medium=social`, `utm_campaign=post-{slug}`.
FE gửi kèm các tham số này khi ping view → thống kê được **kênh nào ra view**.

### 3.5 Đếm view chống spam

`POST /api/public/posts/{slug}/view` với `sessionKey` do FE sinh (lưu
`sessionStorage`). Server băm `IP + User-Agent + muối` thành `visitor_hash`,
bỏ qua nếu cùng `(post_id, visitor_hash)` đã ghi trong **30 phút**. Chỉ khi ghi
mới thì `view_count` mới tăng.

### 3.6 Giữ chân người đọc

Cuối bài: **bài liên quan** = cùng tag (ưu tiên, xếp theo số tag trùng) rồi tới
cùng danh mục, loại bài hiện tại, lấy 6 bài mới nhất. Cộng với khối CTA
(`post_cta`) trỏ về đặt lịch/sản phẩm → chuyển view thành đơn.

---

## 4. API

### 4.1 Công khai (không cần đăng nhập)

| Method | Path | Mô tả |
|---|---|---|
| GET | `/api/public/posts` | phân trang; lọc `categorySlug`, `tagSlug`, `q`, `featured` |
| GET | `/api/public/posts/{slug}` | chi tiết; trả `redirectSlug` nếu slug cũ |
| GET | `/api/public/posts/{slug}/related` | bài liên quan |
| POST | `/api/public/posts/{slug}/view` | ghi nhận lượt xem + UTM |
| POST | `/api/public/posts/{slug}/share` | tăng `share_count` |
| GET | `/api/public/post-categories` | danh mục đang bật |
| GET | `/api/public/post-tags` | tag phổ biến |

### 4.2 Quản trị (`MANAGER`, `ADMIN`; `RECEPTIONIST` soạn và gửi duyệt)

| Method | Path | Mô tả |
|---|---|---|
| GET | `/api/admin/posts` | danh sách, lọc trạng thái/danh mục/từ khoá |
| GET | `/api/admin/posts/{id}` | chi tiết đầy đủ để sửa |
| POST | `/api/admin/posts` | tạo bài |
| PUT | `/api/admin/posts/{id}` | sửa bài |
| PATCH | `/api/admin/posts/{id}/status` | đổi trạng thái (gửi duyệt/duyệt/trả lại/đăng/lưu trữ) |
| DELETE | `/api/admin/posts/{id}` | xoá mềm |
| POST | `/api/admin/posts/upload-image` | tải ảnh lên Cloudinary (thư mục `posts`) |
| GET | `/api/admin/posts/stats` | đếm theo trạng thái + tổng view |
| CRUD | `/api/admin/post-categories` | quản lý danh mục |
| GET/POST/DELETE | `/api/admin/post-tags` | quản lý tag |

### 4.3 Luồng trạng thái

```
DRAFT ──gửi duyệt──> PENDING ──duyệt──> PUBLISHED
  ▲                     │                   │
  └────trả lại──────────┘                   └──> ARCHIVED
DRAFT ──hẹn giờ──> SCHEDULED ──(job 1 phút/lần)──> PUBLISHED
```

`PostPublishScheduler` chạy mỗi phút: bài `SCHEDULED` có `scheduled_at <= now`
sẽ chuyển `PUBLISHED` và đặt `published_at`.

---

## 5. Frontend

### 5.1 Trang công khai (domain khách)

| Route | Trang |
|---|---|
| `/tin-tuc` | `NewsList` — hero bài nổi bật, chip danh mục, ô tìm, lưới bài, phân trang |
| `/tin-tuc/danh-muc/:categorySlug` | cùng `NewsList`, lọc sẵn danh mục |
| `/tin-tuc/tag/:tagSlug` | cùng `NewsList`, lọc sẵn tag |
| `/tin-tuc/:slug` | `NewsDetail` — `<Helmet>` đầy đủ, thanh tiến độ đọc, mục lục, CTA, bài liên quan, nút chia sẻ UTM |

Thêm mục **Tin tức** vào `Header` và `MobileNavbar`.

### 5.2 Trang nhân viên (domain staff)

| Route | Trang | Quyền |
|---|---|---|
| `/post-management` | danh sách bài, lọc trạng thái, đổi trạng thái nhanh | MANAGER, ADMIN, RECEPTIONIST |
| `/post-management/create` | trình soạn thảo | như trên |
| `/post-management/:postId/edit` | trình soạn thảo | như trên |
| `/post-category-config` | quản lý danh mục | MANAGER, ADMIN |

Trình soạn thảo dùng lại kiểu editor `contentEditable` + `execCommand` đã có ở
`BlogFormModal.jsx`, bổ sung: panel SEO (xem trước thẻ Google + thẻ Facebook),
panel CTA, chọn danh mục/tag, hẹn giờ đăng, nút gửi duyệt.

### 5.3 Vệ sinh HTML

HTML người dùng nhập được lọc ở **backend** trước khi lưu: chỉ giữ danh sách thẻ
an toàn, chặn `<script>`, `<iframe>` (trừ YouTube), thuộc tính `on*` và
`javascript:`. Đây là ranh giới tin cậy — không dựa vào lọc phía trình duyệt.

---

## 6. Cấu hình

`application.properties` (đều có giá trị mặc định, chỉ đặt biến môi trường khi
đổi tên miền):

| Khoá | Mặc định | Dùng để |
|---|---|---|
| `news.site.url` | `https://sontaygarage.vn` | dựng `og:url`, `canonical`, link trong sitemap |
| `news.api.url` | `https://api.sontaygarage.vn` | địa chỉ sitemap/RSS khai báo với Google |
| `news.site.name` | `Michelin Sơn Tây` | `og:site_name`, tên publisher trong JSON-LD |
| `news.site.logo` | logo trên trang khách | ảnh dự phòng khi bài chưa có ảnh bìa |
| `news.view.salt` | `mst-gms-news` | muối băm danh tính người xem |

---

## 7. Việc cần làm khi triển khai

1. Khởi động backend → Liquibase tự chạy `006-news-blog.yaml`, tạo 6 bảng
   `post*` và 4 danh mục khởi tạo.
2. Dán `docs/nginx-news-seo.conf` (repo frontend) vào nginx rồi `nginx -t && nginx -s reload`.
   **Chưa làm bước này thì link chia sẻ vẫn chưa ra ảnh** — mọi phần còn lại đã sẵn sàng.
3. Khai báo sitemap trong Google Search Console: `https://api.sontaygarage.vn/seo/sitemap.xml`.
4. Kiểm tra thẻ chia sẻ bằng Facebook Sharing Debugger với một link `/tin-tuc/{slug}`,
   bấm *Scrape Again* để xoá bộ nhớ đệm cũ.
