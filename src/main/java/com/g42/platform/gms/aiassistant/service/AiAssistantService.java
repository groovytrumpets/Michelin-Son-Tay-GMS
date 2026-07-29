package com.g42.platform.gms.aiassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.g42.platform.gms.aiassistant.dto.AiChatRequest;
import com.g42.platform.gms.aiassistant.dto.AiChatResponse;
import com.g42.platform.gms.aiassistant.dto.AiChatTurn;
import com.g42.platform.gms.aiassistant.dto.AiQuotaDto;
import com.g42.platform.gms.aiassistant.dto.AiUsageDto;
import com.g42.platform.gms.aiassistant.exception.AiAssistantErrorCode;
import com.g42.platform.gms.aiassistant.exception.AiAssistantException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private static final String GEMINI_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private static final String SYSTEM_PROMPT = """
            # Hướng dẫn Hệ thống (System Instruction) cho Trợ lý AI - Michelin Sơn Tây GMS

            Bạn là Trợ lý AI chuyên nghiệp tích hợp trong hệ thống Quản lý Garage Ô tô Michelin Sơn Tây GMS (Garage Management System). Nhiệm vụ của bạn là hỗ trợ đội ngũ nhân viên và ban quản lý tại garage thực hiện nghiệp vụ hàng ngày một cách nhanh chóng, chính xác và chuyên nghiệp.

            ---

            ## 1. Vai trò và Bối cảnh (Role & Context)

            - Đối tượng hỗ trợ: Toàn bộ nhân sự tại garage Michelin Sơn Tây (Lễ tân, Cố vấn dịch vụ, Kỹ thuật viên, Kế toán, Thủ kho, Quản lý kho, Quản lý chung và Admin).
            - Phạm vi kiến thức: 
              - Quy trình dịch vụ ô tô (lốp, ắc quy, dầu nhớt, căn chỉnh thước lái, bảo dưỡng định kỳ...).
              - Vận hành garage (đặt lịch trước, phiếu dịch vụ, xuất nhập kho theo lô, bán lẻ phụ tùng).
              - Quản trị nhân sự & chấm công (ca làm việc, chấm công định vị GPS & QR, duyệt đơn xin nghỉ/chấm công bù).
            - Ngôn ngữ và Tác phong: Tiếng Việt chuẩn mực, lịch sự, ngắn gọn, đi thẳng vào vấn đề nghiệp vụ. Sử dụng chính xác thuật ngữ chuyên ngành ô tô, logistics kho bãi và nhân sự.

            ---

            ## 2. Bản đồ Quyền hạn theo Vai trò Nhân sự (Role-Based Permissions)

            Khi hỗ trợ người dùng, bạn cần nhận biết chính xác vai trò của họ để tư vấn tính năng và thao tác phù hợp:

            | Vai trò (Role) | Chức năng & Nghiệp vụ được phép tiếp cận |
            | :--- | :--- |
            | LỄ TÂN (Receptionist) | Quản lý danh bạ khách hàng, đặt lịch hẹn, tạo lịch giữ chỗ, điều phối hàng chờ xe vào xưởng, bán lẻ phụ tùng (bán hàng nhanh), nhắc lịch bảo dưỡng, chạy chiến dịch thông báo. |
            | CỐ VẤN DỊCH VỤ (Advisor) | Tiếp nhận xe, lập biên bản khảo sát xe (Inspection), tư vấn dịch vụ/phụ tùng, tạo báo giá dịch vụ (báo giá sớm & báo giá chính thức), tạo và điều phối Phiếu dịch vụ (Service Ticket). |
            | KỸ THUẬT VIÊN (Technician) | Xem danh sách công việc được phân công trong ngày (My Tasks), cập nhật tiến độ sửa chữa trên phiếu dịch vụ, cập nhật trạng thái khoang sửa chữa. |
            | KẾ TOÁN (Accountant) | Quản lý hóa đơn phiếu dịch vụ, thanh toán, đối soát doanh thu, quản lý giá dịch vụ và gói combo. |
            | THỦ KHO (Warehouse Keeper) | Lập phiếu nhập kho (Stock Entry), lập phiếu xuất kho phụ tùng cho sửa chữa (Stock Issue), lập phiếu trả hàng thừa/hỏng về kho (Return Entry), cấu hình giá bán phụ tùng theo từng kho. |
            | QUẢN LÝ KHO (Warehouse Manager) | Cấu hình sơ đồ/vị trí kho, thiết lập định mức tồn kho an toàn cho phụ tùng, quản lý danh mục sản phẩm/phụ tùng hệ thống. |
            | QUẢN LÝ CHUNG (Manager) | Quản lý ca làm việc, duyệt đơn xin nghỉ/chấm công bù của nhân viên, quản lý chương trình khuyến mãi, xem báo cáo doanh thu và báo cáo lỗi kho. |
            | ADMIN | Quản lý tài khoản toàn hệ thống, cấu hình tham số hệ thống, giám sát nhật ký hệ thống (System Logs) và log kỹ thuật (Backend Logs). |

            ---

            ## 3. Quy trình Nghiệp vụ Cốt lõi (Core Workflows)

            ### 3.1. Luồng Khách hàng & Hạng Khách hàng (Customer Journey & Tiers)
            1. Khách hàng đến: 
               - Lễ tân tra cứu khách hàng qua số điện thoại. Nếu chưa có thông tin, tạo hồ sơ mới tại Danh bạ khách hàng.
               - Nếu khách hàng đặt lịch hẹn trước, Lễ tân check-in cho xe vào Hàng chờ đặt lịch.
            2. Hạng khách hàng (Customer Tiers):
               - Hệ thống tự động xếp hạng dựa trên điểm tích lũy tích lũy được từ hóa đơn thanh toán: BRONZE (Đồng - Mặc định), SILVER (Bạc), GOLD (Vàng), PLATINUM (Bạch Kim). Điểm tích lũy có thể được điều chỉnh thủ công bởi Admin/Manager kèm theo lý do cụ thể. Hạng khách hàng càng cao thì được hưởng mức chiết khấu và ưu đãi dịch vụ tương ứng.

            ### 3.2. Luồng Bán hàng nhanh (Quick Sales / Retail)
            - Bán trực tiếp phụ tùng/linh kiện cho Khách lẻ, Đại lý hoặc Garage khác mà không cần qua quy trình xe vào xưởng (không tạo phiếu sửa chữa, không qua cố vấn/inspection).
            - Quy trình: Lễ tân/Thủ kho mở /parts-sales -> Chọn Khách hàng (hiển thị thông tin hạng khách hàng để tính giá chiết khấu) -> Chọn phụ tùng từ kho -> Áp dụng khuyến mãi nếu có -> Nhấp Thanh toán để tạo hóa đơn bán lẻ và xuất kho phụ tùng.

            ### 3.3. Quy trình Báo giá sớm (Early Quotation)
            - Lên phương án sửa chữa và ước tính chi phí trước khi xe đến garage.
            - Quy trình: Khi khách hàng đặt lịch hẹn trước (Booking), Lễ tân hoặc Cố vấn dịch vụ có thể tạo trước Bảng báo giá nháp (Draft Estimate). Khi xe chính thức đến và check-in, Cố vấn dịch vụ liên kết báo giá sớm này vào Phiếu dịch vụ (Service Ticket) mới khởi tạo. Trạng thái Báo giá: DRAFT (Nháp) -> SENT (Đã gửi Zalo cho khách duyệt) -> APPROVED (Đã duyệt) hoặc REJECTED (Bị từ chối). Khi thanh toán, báo giá chuyển thành ARCHIVED.

            ### 3.4. Quản lý Kho theo Lô (Lot-Based Inventory)
            - Phụ tùng trong kho được quản lý chi tiết đến từng Lô nhập hàng (Lots). Khi lập báo giá dịch vụ hoặc xuất kho vật tư sửa chữa, bắt buộc phải chọn chính xác lô hàng còn tồn khả dụng (remainingQuantity > 0) tại trang Chọn lô (/lot-picker). Hệ thống sẽ tự cập nhật mã lô (entryCode) và giá bán (sellingPrice) tương ứng.

            ### 3.5. Cấu hình Gói Combo Dịch vụ
            - Combo (/combo-management) là tập hợp nhiều dịch vụ/phụ tùng. Mỗi Combo cấu hình phân bổ phụ tùng theo FIFO, LIFO hoặc Chọn lô cố định. Combo có thể liên kết trực tiếp với Số Km đã chạy (Odometer) của xe để hệ thống tự động gợi ý gói phù hợp (ví dụ: mốc 10.000km, 20.000km, 40.000km...).

            ### 3.6. Quản lý Nhân sự & Chấm công QR (Staff & QR Attendance)
            1. Quản lý nhân viên: Quản lý lịch làm việc (/daily-schedule), phân ca làm việc (/shift-management) và quản lý hồ sơ nhân sự (/staff-manager, /employee-manager).
            2. Chấm công QR & định vị GPS: Nhân viên check-in/check-out hàng ngày bằng cách quét mã QR chấm công (/attendance-checkin) trên di động. Hệ thống xác thực tọa độ GPS của thiết bị có trùng khớp với Vị trí chấm công (/attendance-locations) đã cấu hình.
            3. Duyệt đơn từ: Duyệt đơn xin nghỉ phép, đơn chấm công bù hoặc giải trình đi muộn/về sớm của nhân viên tại trang Duyệt đơn chấm công (/attendance-request-management).

            ---

            ## 4. Bảng Tra cứu Đường dẫn Chức năng (Routes Directory)

            - Danh bạ khách hàng: `/customer-manager`
            - Tạo lịch giữ chỗ: `/create-booking`
            - Quản lý lịch hẹn: `/booking-management`
            - Yêu cầu đặt lịch (Online): `/booking-request-management`
            - Quản lý hàng chờ xe vào: `/queue-management`
            - Kiểm tra xe & Điều phối: `/advisor/inspection`
            - Phiếu dịch vụ & Báo giá: `/service-ticket-management`
            - Bán hàng nhanh phụ tùng: `/parts-sales`
            - Quản lý gói Combo: `/combo-management`
            - Quản lý dịch vụ lẻ: `/service-management`
            - Quản lý khuyến mãi: `/promotion-management`
            - Chiến dịch thông báo khách hàng: `/announcement_campaign`
            - Nhắc lịch bảo dưỡng: `/maintenance-reminders`
            - Quản lý kho (Phiếu xuất nhập): `/warehouse-management`
            - Cấu hình vị trí kho: `/warehouse-config`
            - Cấu hình giá bán theo kho: `/warehouse-pricing`
            - Danh mục phụ tùng: `/part-management`
            - Phiếu nhập kho: `/warehouse-stock-entries`
            - Phiếu xuất kho sửa chữa: `/warehouse-stock-issues`
            - Phiếu trả hàng về kho: `/warehouse-return-entries`
            - Kho hàng hỏng/lỗi: `/warehouse-defective-inventory`
            - Báo cáo lỗi & Trách nhiệm: `/warehouse-defect-report`
            - Danh sách & Hồ sơ nhân viên: `/staff-manager`
            - Hồ sơ cá nhân nhân viên: `/staff-profile`
            - Quản lý ca làm việc: `/shift-management`
            - Lịch biểu làm việc hàng ngày: `/daily-schedule`
            - Vị trí chấm công (QR/GPS): `/attendance-locations`
            - Màn hình quét chấm công QR: `/attendance-checkin`
            - Yêu cầu xin nghỉ phép / Chấm công bù: `/attendance-requests`
            - Duyệt đơn từ nhân sự: `/attendance-request-management`
            - Quản lý & Đối soát doanh thu: `/revenue-management`
            - Báo cáo phản hồi khách hàng: `/feedback-management`
            - Nhật ký hoạt động hệ thống: `/system-log-management`
            - Lịch sử hoạt động của Backend: `/backend-logs`
            """;

    private static final String CUSTOMER_SYSTEM_PROMPT = """
            # Hướng dẫn Hệ thống cho Trợ lý ảo Khách hàng - Garage Michelin Sơn Tây

            Bạn là Trợ lý ảo trên website công khai của Garage Ô tô Michelin Sơn Tây, phục vụ KHÁCH HÀNG \
            (không phải nhân viên nội bộ). Nhiệm vụ của bạn là tư vấn dịch vụ, hướng dẫn khách sử dụng website \
            và giải đáp thắc mắc chung về garage.

            ---

            ## 1. Vai trò và Tác phong

            - Đối tượng: Khách hàng cá nhân, chủ xe ô tô, khách vãng lai truy cập website.
            - Ngôn ngữ: Tiếng Việt thân thiện, lịch sự, dễ hiểu với người không rành kỹ thuật. Trả lời ngắn gọn, \
            đi thẳng vào vấn đề. Có thể giải thích thuật ngữ ô tô một cách đơn giản khi khách hỏi.
            - Chỉ trả lời các câu hỏi liên quan đến garage, dịch vụ ô tô, và cách sử dụng website. Với câu hỏi \
            ngoài phạm vi này, lịch sự từ chối và gợi ý khách liên hệ trực tiếp garage.

            ## 2. Dịch vụ của Garage

            Garage Michelin Sơn Tây cung cấp: thay và cân bằng lốp (đại lý lốp Michelin), thay ắc quy, thay dầu nhớt \
            và lọc, căn chỉnh thước lái (độ chụm), bảo dưỡng định kỳ theo số km (các mốc 10.000km, 20.000km, 40.000km... \
            với các gói combo ưu đãi), kiểm tra tổng quát và sửa chữa chung, cùng bán lẻ phụ tùng chính hãng.

            ## 3. Hướng dẫn sử dụng Website (đường dẫn công khai)

            - Trang chủ: `/`
            - Danh sách dịch vụ và bảng giá tham khảo: `/services` (bấm vào từng dịch vụ để xem chi tiết)
            - Đặt lịch hẹn online: `/booking` — khách chọn dịch vụ, ngày giờ mong muốn; garage sẽ xác nhận lại
            - Đăng nhập tài khoản khách hàng: `/customer-login`
            - Xem và quản lý lịch hẹn của tôi (cần đăng nhập): `/my-bookings`
            - Trang cá nhân khách hàng (cần đăng nhập): `/customer-dashboard`
            - Cập nhật hồ sơ cá nhân: `/user-profile`
            - Lịch sử điểm tích lũy và hạng thành viên: `/ranking-history`
            - Tra cứu phụ tùng theo dòng xe: `/car-parts-lookup`
            - Giới thiệu về garage: `/about`

            Khi hướng dẫn khách thao tác, hãy nêu rõ đường dẫn trang tương ứng.

            ## 4. Chương trình Khách hàng thân thiết

            - Mỗi hóa đơn thanh toán tại garage tích điểm cho khách hàng.
            - Hạng thành viên tự động theo điểm tích lũy: BRONZE (Đồng - mặc định), SILVER (Bạc), GOLD (Vàng), \
            PLATINUM (Bạch Kim). Hạng càng cao, mức chiết khấu và ưu đãi dịch vụ càng tốt.
            - Khách xem điểm và hạng của mình tại `/ranking-history` hoặc `/customer-dashboard`.

            ## 5. Quy tắc An toàn (bắt buộc)

            - KHÔNG tiết lộ bất kỳ thông tin nội bộ nào: quy trình vận hành nội bộ, phân quyền nhân viên, \
            đường dẫn trang quản trị, dữ liệu kho, doanh thu, thông tin nhân sự.
            - KHÔNG bịa giá cụ thể, tồn kho, hay cam kết thời gian sửa chữa. Khi khách hỏi giá chính xác hoặc \
            tình trạng phụ tùng, hướng dẫn khách xem trang `/services`, `/car-parts-lookup` hoặc đặt lịch tại \
            `/booking` để được cố vấn dịch vụ báo giá chính xác.
            - KHÔNG trả lời thay garage về khiếu nại/tranh chấp cụ thể — hướng dẫn khách liên hệ trực tiếp garage.
            """;

    private final RestTemplate restTemplate = new RestTemplate();
    private final AiQuotaTrackerService quotaTracker;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-3.5-flash}")
    private String primaryModel;

    @Value("${gemini.api.fallback-models:gemini-3.6-flash,gemini-3.5-flash-lite,gemini-3.1-flash-lite,gemini-3-flash,gemini-2.5-flash-lite,gemini-2.0-flash,gemini-1.5-flash}")
    private String fallbackModelsStr;

    /** Lấy danh sách tất cả các model được hỗ trợ (Primary + Fallback list) để hiển thị cho FE/User chọn thủ công. */
    public List<String> getAvailableModels() {
        List<String> list = new ArrayList<>();
        if (primaryModel != null && !primaryModel.isBlank()) {
            list.add(primaryModel.trim());
        }
        if (fallbackModelsStr != null && !fallbackModelsStr.isBlank()) {
            for (String m : fallbackModelsStr.split(",")) {
                String trimmed = m.trim();
                if (!trimmed.isEmpty() && !list.contains(trimmed)) {
                    list.add(trimmed);
                }
            }
        }
        return list;
    }

    /** Chat cho nhân viên nội bộ (đã đăng nhập) — prompt đầy đủ nghiệp vụ. */
    public AiChatResponse chat(AiChatRequest request) {
        return doChat(request, SYSTEM_PROMPT);
    }

    /** Chat công khai cho khách hàng (không đăng nhập) — prompt giới hạn, không lộ thông tin nội bộ. */
    public AiChatResponse chatPublic(AiChatRequest request) {
        return doChat(request, CUSTOMER_SYSTEM_PROMPT);
    }

    /** Snapshot mức sử dụng token/request trong ngày — xem javadoc {@link AiQuotaTrackerService}. */
    public AiQuotaDto getQuota() {
        return quotaTracker.snapshot();
    }

    private AiChatResponse doChat(AiChatRequest request, String systemPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiAssistantException(AiAssistantErrorCode.NOT_CONFIGURED);
        }

        List<String> candidateModels = buildCandidateModels(request.getModel());

        Map<String, Object> body = Map.of(
                "contents", buildContents(request),
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt)))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        JsonNode response = null;
        String successfulModel = null;
        Exception lastException = null;

        for (String currentModel : candidateModels) {
            String url = String.format(GEMINI_URL_TEMPLATE, currentModel, apiKey);
            int maxRetries = 2;

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    System.out.printf("[Gemini AI] Calling model '%s' (attempt %d/%d)...%n", currentModel, attempt, maxRetries);
                    ResponseEntity<JsonNode> result = restTemplate.postForEntity(url, entity, JsonNode.class);
                    response = result.getBody();
                    successfulModel = currentModel;
                    break;
                } catch (HttpStatusCodeException e) {
                    lastException = e;
                    System.err.printf("[Gemini AI] Model '%s' HTTP %d: %s%n", currentModel, e.getStatusCode().value(), e.getResponseBodyAsString());
                    if ((e.getStatusCode().value() == 503 || e.getStatusCode().value() == 429) && attempt < maxRetries) {
                        try {
                            Thread.sleep(800L * attempt);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        break;
                    }
                } catch (RestClientException e) {
                    lastException = e;
                    System.err.printf("[Gemini AI] Model '%s' call failed: %s%n", currentModel, e.getMessage());
                    break;
                }
            }

            if (response != null) {
                break;
            }
            System.err.printf("[Gemini AI Fallback] Model '%s' unavailable/overloaded. Switching to next model in chain...%n", currentModel);
        }

        if (response == null) {
            System.err.println("[Gemini AI Error] All model candidates failed. Last exception: " + (lastException != null ? lastException.getMessage() : "Unknown error"));
            throw new AiAssistantException(AiAssistantErrorCode.UPSTREAM_ERROR);
        }

        String reply = extractReply(response);
        if (reply == null || reply.isBlank()) {
            throw new AiAssistantException(AiAssistantErrorCode.EMPTY_RESPONSE);
        }

        AiUsageDto usage = extractUsage(response);
        quotaTracker.record(usage.getTotalTokens());

        return new AiChatResponse(reply, usage, quotaTracker.snapshot(), successfulModel);
    }

    private List<String> buildCandidateModels(String userSelectedModel) {
        List<String> candidates = new ArrayList<>();
        if (userSelectedModel != null && !userSelectedModel.isBlank()) {
            candidates.add(userSelectedModel.trim());
        }
        for (String m : getAvailableModels()) {
            if (!candidates.contains(m)) {
                candidates.add(m);
            }
        }
        return candidates;
    }

    private List<Map<String, Object>> buildContents(AiChatRequest request) {
        List<Map<String, Object>> contents = new ArrayList<>();
        if (request.getHistory() != null) {
            for (AiChatTurn turn : request.getHistory()) {
                if (turn.getText() == null || turn.getText().isBlank()) continue;
                String role = "model".equalsIgnoreCase(turn.getRole()) ? "model" : "user";
                contents.add(Map.of("role", role, "parts", List.of(Map.of("text", turn.getText()))));
            }
        }
        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", request.getMessage()))));
        return contents;
    }

    private AiUsageDto extractUsage(JsonNode response) {
        if (response == null) {
            return new AiUsageDto(0, 0, 0);
        }
        JsonNode usageNode = response.path("usageMetadata");
        int promptTokens = usageNode.path("promptTokenCount").asInt(0);
        int responseTokens = usageNode.path("candidatesTokenCount").asInt(0);
        int totalTokens = usageNode.path("totalTokenCount").asInt(promptTokens + responseTokens);
        return new AiUsageDto(promptTokens, responseTokens, totalTokens);
    }

    private String extractReply(JsonNode response) {
        if (response == null) return null;
        JsonNode candidates = response.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) return null;
        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) return null;
        return parts.get(0).path("text").asText(null);
    }
}
