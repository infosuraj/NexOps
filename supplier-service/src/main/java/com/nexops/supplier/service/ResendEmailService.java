package com.nexops.supplier.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.nexops.supplier.entity.PurchaseOrder;
import java.util.Map;

@Service
@Slf4j
public class ResendEmailService {

    @Value("${resend.api.key}")
    private String apiKey;

    @Value("${resend.api.from}")
    private String fromEmail;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendPurchaseOrderEmail(PurchaseOrder po, String supplierEmail) {
        if ("demo-mode".equals(apiKey)) {
            log.info("[DEMO] PO email to supplier {} | PO-{} | {} units of {}",
                po.getSupplierName(), po.getId(), po.getQuantityOrdered(), po.getProductName());
            return;
        }

        String html = """
            <div style="font-family:Helvetica,sans-serif;max-width:600px;margin:0 auto">
              <div style="background:#111;padding:20px;border-radius:8px 8px 0 0">
                <h2 style="color:#fff;margin:0">NexOps — Purchase Order</h2>
              </div>
              <div style="background:#fff;padding:24px;border:1px solid #e8e8e8;border-radius:0 0 8px 8px">
                <p>Dear <b>%s</b>,</p>
                <p>Please find below our purchase order details:</p>
                <table style="width:100%%;border-collapse:collapse;margin:16px 0">
                  <tr style="background:#f3f3f3">
                    <td style="padding:10px;font-weight:700">PO Number</td>
                    <td style="padding:10px">PO-%04d</td>
                  </tr>
                  <tr>
                    <td style="padding:10px;font-weight:700">Product</td>
                    <td style="padding:10px">%s</td>
                  </tr>
                  <tr style="background:#f3f3f3">
                    <td style="padding:10px;font-weight:700">Quantity Required</td>
                    <td style="padding:10px">%d units</td>
                  </tr>
                  <tr>
                    <td style="padding:10px;font-weight:700">Estimated Value</td>
                    <td style="padding:10px">₹%s</td>
                  </tr>
                  <tr style="background:#f3f3f3">
                    <td style="padding:10px;font-weight:700">Expected Delivery</td>
                    <td style="padding:10px">%s</td>
                  </tr>
                </table>
                <p>Please confirm this order at your earliest convenience.</p>
                <p style="font-size:12px;color:#a0a0a0">
                  This is an automated order from the NexOps Supplier Management System.
                </p>
              </div>
            </div>
            """.formatted(
                po.getSupplierName(), po.getId(), po.getProductName(),
                po.getQuantityOrdered(),
                po.getEstimatedCost() != null ? po.getEstimatedCost().toPlainString() : "TBD",
                po.getExpectedDelivery() != null ? po.getExpectedDelivery().toLocalDate().toString() : "TBD"
            );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                "from", fromEmail,
                "to", new String[]{supplierEmail},
                "subject", "Purchase Order PO-" + String.format("%04d", po.getId()) + " — " + po.getProductName(),
                "html", html
            );

            restTemplate.exchange(
                "https://api.resend.com/emails",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
            );
            log.info("PO email sent to supplier {} for PO-{}", po.getSupplierName(), po.getId());
        } catch (Exception e) {
            log.error("Failed to send PO email: {}", e.getMessage());
        }
    }
}
