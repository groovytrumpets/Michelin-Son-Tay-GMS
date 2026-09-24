package com.g42.platform.gms.estimation.infrastructure;


import com.g42.platform.gms.common.util.Qty;
import com.g42.platform.gms.estimation.api.dto.UsedEstimateItemDto;
import com.g42.platform.gms.estimation.api.internal.EstimateInternalApi;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.domain.entity.EstimateItem;
import com.g42.platform.gms.estimation.domain.entity.ItemCategory;
import com.g42.platform.gms.estimation.domain.exception.EstimateErrorCode;
import com.g42.platform.gms.estimation.domain.exception.EstimateException;
import com.g42.platform.gms.estimation.domain.repository.EstimateItemRepository;
import com.g42.platform.gms.estimation.domain.repository.EstimateRepository;
import com.g42.platform.gms.estimation.domain.repository.ItemCategoryRepository;
import com.g42.platform.gms.estimation.infrastructure.entity.EstimateItemJpa;
import com.g42.platform.gms.estimation.infrastructure.entity.EstimateJpa;
import com.g42.platform.gms.estimation.infrastructure.entity.ServiceReminderJpa;
import com.g42.platform.gms.estimation.infrastructure.entity.StockAllocationJpa;
import com.g42.platform.gms.estimation.infrastructure.mapper.EstimateJpaMapper;
import com.g42.platform.gms.estimation.infrastructure.repository.EstimateItemRepositoryJpa;
import com.g42.platform.gms.estimation.infrastructure.repository.EstimateRepositoryJpa;
import com.g42.platform.gms.estimation.infrastructure.repository.ServiceRemindJpaRepo;
import com.g42.platform.gms.estimation.infrastructure.repository.StockAllocationRepositoryJpa;
import com.g42.platform.gms.promotion.api.internal.PromotionInternalApi;
import com.g42.platform.gms.promotion.domain.entity.Promotion;
import com.g42.platform.gms.warehouse.api.internal.WarehouseInternalApi;
import com.g42.platform.gms.warehouse.domain.entity.StockIssue;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockIssueJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.StockIssueJpaRepo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class EstimateInternalApiImpl implements EstimateInternalApi {
    @Autowired
    private EstimateRepositoryJpa estimateRepositoryJpa;
    @Autowired
    private EstimateJpaMapper estimateJpaMapper;
    @Autowired
    private ServiceRemindJpaRepo serviceRemindJpaRepo;
    @Autowired
    private StockAllocationRepositoryJpa stockAllocationJpaRepo;
    @Autowired
    private EstimateItemRepositoryJpa estimateItemRepositoryJpa;
    @Autowired
    private PromotionInternalApi promotionInternalApi;
    @Autowired
    private EstimateRepository estimateRepository;
    @Autowired
    private EstimateItemRepository estimateItemRepository;
    @Autowired
    private WarehouseInternalApi warehouseInternalApi;
    @Autowired
    private StockIssueJpaRepo stockIssueJpaRepo;
    @Autowired
    private ItemCategoryRepository itemCategoryRepository;

    @Override
    public List<Estimate> findAllByServiceTicketId(List<Integer> ticketIds) {
        return estimateRepositoryJpa.findByServiceTicketIdsAndVersionTop(ticketIds).stream().map(estimateJpaMapper::toDomain).toList();
    }

    @Override
    public List<UsedEstimateItemDto> findUsedItemsHistoryByServiceTicketIds(List<Integer> ticketIds) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            return List.of();
        }

        // Estimate mới nhất (theo version) của mỗi service ticket
        List<EstimateJpa> latestEstimates = estimateRepositoryJpa.findByServiceTicketIdsAndVersionTop(ticketIds);
        if (latestEstimates.isEmpty()) {
            return List.of();
        }
        Map<Integer, Integer> estimateIdToTicketId = latestEstimates.stream()
                .collect(Collectors.toMap(EstimateJpa::getId, EstimateJpa::getServiceTicketId));

        List<EstimateItemJpa> items = estimateItemRepositoryJpa.findByEstimateIds(new ArrayList<>(estimateIdToTicketId.keySet()));

        List<Integer> categoryIds = items.stream()
                .map(EstimateItemJpa::getItemCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Integer, ItemCategory> categoryMap = categoryIds.isEmpty()
                ? Map.of()
                : itemCategoryRepository.findAllById(categoryIds).stream()
                        .collect(Collectors.toMap(ItemCategory::getId, wc -> wc));

        return items.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getIsRemoved()))
                .map(item -> {
                    UsedEstimateItemDto dto = new UsedEstimateItemDto();
                    dto.setServiceTicketId(estimateIdToTicketId.get(item.getEstimateId()));
                    dto.setItemName(item.getItemName());
                    ItemCategory category = categoryMap.get(item.getItemCategoryId());
                    dto.setCategoryName(category != null ? category.getCategoryName() : null);
                    dto.setQuantity(item.getQuantity());
                    dto.setUnitPrice(item.getUnitPrice());
                    dto.setFinalPrice(item.getFinalPrice());
                    dto.setIsGift(item.getIsGift());
                    return dto;
                })
                .toList();
    }

    @Override
    public Estimate findLatestByServiceTicketId(Integer serviceTicketId) {
        if (serviceTicketId == null) {
            return null;
        }
        return estimateRepositoryJpa.findTopByServiceTicketIdOrderByVersionDesc(serviceTicketId)
                .map(estimateJpaMapper::toDomain)
                .orElse(null);
    }

    @Override
    public Estimate findById(Integer estimateId) {
        if (estimateId == null) {
            return null;
        }
        return estimateRepositoryJpa.findById(estimateId)
                .map(estimateJpaMapper::toDomain)
                .orElse(null);
    }

    @Override
    public void linkEstimateToServiceTicket(Integer estimateId, Integer serviceTicketId) {
        if (estimateId == null || serviceTicketId == null) {
            return;
        }
        EstimateJpa estimateJpa = estimateRepositoryJpa.findById(estimateId).orElse(null);
        if (estimateJpa == null) {
            return;
        }
        estimateJpa.setServiceTicketId(serviceTicketId);
        estimateRepositoryJpa.save(estimateJpa);
    }

    @Override
    public void updateBookingToRemindById(Integer reminderId, Integer bookingId) {
        ServiceReminderJpa sr = serviceRemindJpaRepo.findById(reminderId).orElse(null);
        if (sr==null||bookingId==null) {
            System.err.println("INVALID REMINDER/BOOKING ID");
            return;
        }
        sr.setStatus("BOOKED");
        sr.setBookingId(bookingId);
        serviceRemindJpaRepo.save(sr);
    }

    @Override
    public Integer releaseEstimate(Integer allocationId, BigDecimal returnQuantity, Integer staffId) {
        StockAllocationJpa stockAllocation = stockAllocationJpaRepo.findById(allocationId).orElse(null);
        if (stockAllocation==null) {
            throw new EstimateException("Allocation:"+allocationId+" 404",EstimateErrorCode.BAD_REQUEST);
        }
        EstimateItemJpa estimateItemJpa = estimateItemRepositoryJpa.findById(stockAllocation.getEstimateItemId()).orElse(null);
        if (estimateItemJpa!=null) {
            EstimateItemJpa nextRevision = estimateItemRepositoryJpa.findByRevisedFromItemId(estimateItemJpa.getId());
            while (nextRevision!=null) {
                estimateItemJpa =  nextRevision;
                nextRevision = estimateItemRepositoryJpa.findByRevisedFromItemId(estimateItemJpa.getId());
            }
        }
        if (estimateItemJpa==null || estimateItemJpa.getQuantity()==null) {
            throw new EstimateException("EstimateItem of this Allocation:"+allocationId+" 404",EstimateErrorCode.BAD_REQUEST);
        }
        BigDecimal oldQty = estimateItemJpa.getQuantity();
        if (Qty.eq(oldQty, returnQuantity)) {
            estimateItemJpa.setIsChecked(false);
            EstimateItemJpa saved = estimateItemRepositoryJpa.save(estimateItemJpa);
            return saved.getId();
        }
//        //todo: validate buy x get y
//        List<EstimateItemJpa> attachedGifts = estimateItemRepositoryJpa.findByEstimateIdAndTriggeredByItemId(estimateItemJpa.getEstimateId(), estimateItemJpa.getId());
//        if (attachedGifts!=null && !attachedGifts.isEmpty()) {
//            for (EstimateItemJpa gift : attachedGifts) {
//                if (gift.getPromotionId()!=null) {
//                    Promotion promotion = promotionInternalApi.findById(gift.getPromotionId());
//                    if (promotion!=null) {
//                        BigDecimal newTriggerQty = oldQty - returnQuantity;
//                        BigDecimal validGiftQty = (newTriggerQty/promotion.getBuyQuantity())*promotion.getGetQuantity();
//                        int giftToReturn = gift.getQuantity()-validGiftQty;
//                        if (giftToReturn>0){
//
//                        }
//                    }
//                }
//            }
//        }

        EstimateItemJpa returnEstimate = new EstimateItemJpa();
        BeanUtils.copyProperties(estimateItemJpa,returnEstimate,"id");

        estimateItemJpa.setQuantity(Qty.sub(estimateItemJpa.getQuantity(), returnQuantity));
        recalculateItemPrices(estimateItemJpa,oldQty);
        estimateItemRepositoryJpa.save(estimateItemJpa);

        returnEstimate.setIsChecked(false);
        returnEstimate.setQuantity(returnQuantity);
        recalculateItemPrices(returnEstimate,oldQty);
        EstimateItemJpa saved = estimateItemRepositoryJpa.save(returnEstimate);
        return saved.getId();
    }

    @Override
    public void calculateAndLockGrossProfit(Integer serviceTicketId) {
        Estimate estimate = estimateRepository.findEstimateByServiceIdAndLatestVerson(serviceTicketId);
        if (estimate == null) {
            return;
        }
        List<EstimateItem> items = estimateItemRepository.findByEstimateId(estimate.getId());

        BigDecimal totalEstimateCost = BigDecimal.ZERO;
        BigDecimal totalEstimateGrossProfit = BigDecimal.ZERO;

        for (EstimateItem item : items) {
            if (item.getIsRemoved()) continue;

            BigDecimal unitCost = BigDecimal.ZERO;

            // 2. Chỉ tính giá vốn cho phụ tùng/vật tư (bỏ qua công thợ nếu công thợ không có kho)
            if (item.getItemId() != null && item.getWarehouseId() != null) {
                // Lấy giá vốn (sử dụng hàm Fallback FIFO cũ của bạn hoặc API từ kho)
                StockAllocationJpa stockAllocation = stockAllocationJpaRepo.findByEstimateItemId(item.getId());
                StockIssueJpa stockIssueJpa = stockIssueJpaRepo.findById(stockAllocation.getIssueId()).orElse(null);
                if (stockIssueJpa==null) {
                    continue;
                }
                BigDecimal cost = warehouseInternalApi.findLatesFallBackPrice(item.getItemId(), item.getWarehouseId());
                if (cost != null) {
                    unitCost = cost;
                }
            }

            // 3. Tính toán tiền nong cho dòng này
            BigDecimal quantity = Qty.nz(item.getQuantity());
            BigDecimal totalCost = unitCost.multiply(quantity);

            // Lợi nhuận = Giá bán cuối cùng (đã trừ discount) - Tổng giá vốn
            BigDecimal itemGrossProfit = item.getFinalPrice().subtract(totalCost);

            // 4. Lưu vào Estimate Item
            item.setGrossProfit(itemGrossProfit); // Thêm field gross_profit
            estimateItemRepository.save(item);

            // 5. Cộng dồn lên biến tổng
            totalEstimateCost = totalEstimateCost.add(totalCost);
            totalEstimateGrossProfit = totalEstimateGrossProfit.add(itemGrossProfit);
        }
        estimate.setGrossProfit(totalEstimateGrossProfit);
        estimateRepository.save(estimate);
    }
//
//    @Override
//    public void validatePromotion(Map<Integer, Integer> returnAllocationMap) {
//        // 1. Lấy tất cả thông tin Allocation đang được yêu cầu trả
//        List<StockAllocationJpa> allocations = stockAllocationJpaRepo.findAllById(returnAllocationMap.keySet());
//
//        // Tạo Map chuyển đổi từ EstimateItemId -> Số lượng khách ĐANG MUỐN TRẢ
//        Map<Integer, BigDecimal> returningEstimateItemQtyMap = new HashMap<>();
//        for (StockAllocationJpa alloc : allocations) {
//            returningEstimateItemQtyMap.put(alloc.getEstimateItemId(), returnAllocationMap.get(alloc.getAllocationId()));
//        }
//
//        // 2. Quét từng món hàng khách đang trả xem có phải là hàng mồi không
//        for (Map.Entry<Integer, Integer> entry : returningEstimateItemQtyMap.entrySet()) {
//            Integer estimateItemId = entry.getKey();
//            BigDecimal returnQty = entry.getValue();
//
//            EstimateItemJpa estimateItemJpa = estimateItemRepositoryJpa.findById(estimateItemId).orElse(null);
//            if (estimateItemJpa == null) continue;
//
//            // Tìm quà tặng đính kèm
//            List<EstimateItemJpa> attachedGifts = estimateItemRepositoryJpa.findByEstimateIdAndTriggeredByItemId(
//                    estimateItemJpa.getEstimateId(), estimateItemJpa.getId()
//            );
//
//            if (attachedGifts != null && !attachedGifts.isEmpty()) {
//                for (EstimateItemJpa gift : attachedGifts) {
//                    if (gift.getPromotionId() != null) {
//                        Promotion promotion = promotionInternalApi.findById(gift.getPromotionId());
//                        if (promotion != null) {
//                            BigDecimal oldQty = estimateItemJpa.getQuantity();
//                            BigDecimal newTriggerQty = oldQty - returnQty;
//
//                            // Tính lượng quà khách ĐƯỢC PHÉP giữ lại
//                            BigDecimal validGiftQty = (newTriggerQty / promotion.getBuyQuantity()) * promotion.getGetQuantity();
//
//                            // Tính lượng quà BẮT BUỘC PHẢI TRẢ
//                            int requiredGiftReturn = gift.getQuantity() - validGiftQty;
//
//                            if (requiredGiftReturn > 0) {
//                                // 3. ĐỐI CHIẾU VỚI GIỎ TRẢ HÀNG
//                                // Kiểm tra xem trong cái list khách trả, có cái quà này không?
//                                BigDecimal giftQtyInReturnRequest = returningEstimateItemQtyMap.getOrDefault(gift.getId(), BigDecimal.ZERO);
//
//                                // Nếu lượng quà có trong giỏ trả hàng ÍT HƠN lượng quà bắt buộc phải trả -> Lỗi!
//                                if (giftQtyInReturnRequest < requiredGiftReturn) {
//                                    BigDecimal missingQty = requiredGiftReturn - giftQtyInReturnRequest;
//                                    throw new EstimateException(
//                                            "Vi phạm khuyến mãi! Khách trả " + returnQty + " '" + estimateItemJpa.getItemName() +
//                                                    "' nên bị rớt mốc nhận quà. Cố vấn dịch vụ cần yêu cầu khách trả thêm " + missingQty + " '" + gift.getItemName() + "' vào phiếu này!",
//                                            EstimateErrorCode.BAD_REQUEST
//                                    );
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }

    private void recalculateItemPrices(EstimateItemJpa estimateItemJpa, BigDecimal oldQuantity) {
        if (estimateItemJpa.getUnitPrice()==null || estimateItemJpa.getQuantity()==null) return;
        //todo calculate price based on promotions, tax and quantity
        BigDecimal totalPrice = estimateItemJpa.getUnitPrice().multiply(estimateItemJpa.getQuantity());
        estimateItemJpa.setTotalPrice(totalPrice);
        //discount
        BigDecimal newDiscount = BigDecimal.ZERO;
        if (estimateItemJpa.getDiscountAmount()!=null) {
            BigDecimal unitDiscount = estimateItemJpa.getDiscountAmount().divide(oldQuantity, 2, RoundingMode.HALF_UP);
            newDiscount = unitDiscount.multiply(estimateItemJpa.getQuantity());
        }
        estimateItemJpa.setDiscountAmount(newDiscount);
        // Giảm giá tay (số tiền) cũng chia theo số lượng như phần giảm chung, để hai dòng
        // tách ra khi hoàn hàng không cùng giữ nguyên số giảm của cả dòng cũ.
        if (estimateItemJpa.getManualDiscountAmount() != null) {
            estimateItemJpa.setManualDiscountAmount(newDiscount);
        }
        BigDecimal finalPrice = totalPrice.subtract(newDiscount);
        //tax
        BigDecimal newTaxAmount = BigDecimal.ZERO;
        if (estimateItemJpa.getAppliedTaxRate()!=null) {
            BigDecimal taxRate = estimateItemJpa.getAppliedTaxRate().divide(BigDecimal.valueOf(100),4, RoundingMode.HALF_UP);
            newTaxAmount = finalPrice.multiply(taxRate).setScale(0, RoundingMode.HALF_UP);
        }
        estimateItemJpa.setTaxAmount(newTaxAmount);
        finalPrice = finalPrice.add(estimateItemJpa.getTaxAmount());
        estimateItemJpa.setFinalPrice(finalPrice);

    }
}
