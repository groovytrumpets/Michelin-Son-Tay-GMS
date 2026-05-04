package com.g42.platform.gms.booking;

import com.g42.platform.gms.warehouse.app.service.inventory.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class InventoryServiceTest {

        @Autowired
        private InventoryService inventoryService;

        @Test
        void testRaceCondition() throws InterruptedException {
            int numberOfThreads = 100;
            ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
            CountDownLatch latch = new CountDownLatch(1); // Còi báo hiệu xuất phát
            CountDownLatch finishLatch = new CountDownLatch(numberOfThreads); // Đợi tất cả chạy xong

            for (int i = 0; i < numberOfThreads; i++) {
                executorService.execute(() -> {
                    try {
                        latch.await(); // Tất cả các luồng đứng đây đợi...

                        // Gọi hàm mua 1 lốp
                        inventoryService.increaseReservedQuantity(1029, 1, 1);
                    } catch (Exception e) {
                        System.out.println("Bị chặn do hết hàng hoặc lỗi: " + e.getMessage());
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }

            // KỊCH BẢN TEST:
            // Set DB kho ban đầu có đúng 10 lốp GS Prime (Available = 10, Reserved = 0)

            latch.countDown(); // Phát lệnh xuất phát! 100 luồng đồng loạt tấn công DB
            finishLatch.await(); // Chờ 100 thằng chạy xong

            // Kiểm tra kết quả trong DB:
            // Nếu code chuẩn: Sẽ chỉ có đúng 10 request thành công, Reserved tăng lên 10, 90 request còn lại văng Exception.
            // Nếu code lởm (Race Condition): Reserved có thể lớn hơn 10 (âm kho).
        }

}
