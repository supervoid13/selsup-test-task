package com.uneev.selsuptest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Data
public class CrptApi {

    private Semaphore semaphore;
    private TimeUnit timeUnit;

    private final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.semaphore = new Semaphore(requestLimit);
    }


    public void createDocument(Document document, String signature) throws InterruptedException, IOException {
        while (true) {
            if (semaphore.tryAcquire(1, timeUnit)) {
                executeRequest(document);
                break;
            } else {
                System.out.println("Too many requests, please wait");
            }
        }
    }

    private void executeRequest(Document document) throws JsonProcessingException, IOException {
        final HttpPost httpPost = new HttpPost(URL);
        final String json = parseDocumentToJson(document);
        final StringEntity entity = new StringEntity(
                json,
                ContentType.APPLICATION_JSON
        );

        httpPost.setEntity(entity);

        ResponseHandler< String > responseHandler = response -> {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity httpEntity = response.getEntity();
                return httpEntity != null ? EntityUtils.toString(httpEntity) : null;
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        };

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String responseBody = client.execute(httpPost, responseHandler);
        }
    }

    private String parseDocumentToJson(Document document) throws JsonProcessingException {
        return objectMapper.writeValueAsString(document);
    }
}

@Data
@AllArgsConstructor
class Document {

    Description description;

    @JsonProperty("doc_id")
    String id;

    @JsonProperty("doc_status")
    String status;

    @JsonProperty("doc_type")
    MyDocType docType;

    Boolean importRequest;

    @JsonProperty("owner_inn")
    String ownerInn;

    @JsonProperty("participant_inn")
    String participantInn;

    @JsonProperty("producer_inn")
    String producerInn;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("production_date")
    LocalDate productionDate;

    @JsonProperty("production_type")
    String productionType;

    List<Product> products;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("reg_date")
    LocalDate regDate;

    @JsonProperty("reg_number")
    String regNumber;


    @Data
    @AllArgsConstructor
    static class Description {
        String participantInn;
    }

    enum MyDocType {
        LP_INTRODUCE_GOODS
    }
}

@Data
@AllArgsConstructor
class Product {

    @JsonProperty("certificate_document")
    String certificateDocument;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("certificate_document_date")
    LocalDate certificateDocumentDate;

    @JsonProperty("certificate_document_number")
    String certificateDocumentNumber;

    @JsonProperty("owner_inn")
    String ownerInn;

    @JsonProperty("producer_inn")
    String producerInn;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("production_date")
    LocalDate productionDate;

    @JsonProperty("tnved_code")
    String tnvedCode;

    @JsonProperty("uit_code")
    String uitCode;

    @JsonProperty("uitu_code")
    String uituCode;
}