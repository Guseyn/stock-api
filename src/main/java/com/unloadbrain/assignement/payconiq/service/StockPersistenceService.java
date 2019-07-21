package com.unloadbrain.assignement.payconiq.service;

import com.unloadbrain.assignement.payconiq.domain.model.Stock;
import com.unloadbrain.assignement.payconiq.domain.repository.StockRepository;
import com.unloadbrain.assignement.payconiq.dto.reqeust.CreateStockRequest;
import com.unloadbrain.assignement.payconiq.dto.reqeust.UpdateStockRequest;
import com.unloadbrain.assignement.payconiq.dto.response.IdentityResponse;
import com.unloadbrain.assignement.payconiq.dto.response.StockResponse;
import com.unloadbrain.assignement.payconiq.dto.response.StocksResponse;
import com.unloadbrain.assignement.payconiq.exception.StockNotFoundException;
import com.unloadbrain.assignement.payconiq.util.DateTimeUtil;
import com.unloadbrain.assignement.payconiq.util.UuidUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.cassandra.core.query.CassandraPageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class StockPersistenceService {

    private StockRepository stockRepository;
    private UuidUtil uuidUtil;
    private DateTimeUtil dateTimeUtil;

    public StocksResponse getStocks(int pageNo, int pageSize) {

        Slice<Stock> slice = getPage(pageNo, pageSize);

        List<StockResponse> stockResponseList = slice.getContent().stream()
                .map(stock -> buildStockResponse(stock))
                .collect(Collectors.toList());

        return StocksResponse.builder()
                .data(stockResponseList)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .build();
    }

    private Slice<Stock> getPage(int pageNo, int pageSize) {

        /**
         * For some reason, CassandraRepository can not find result page unless it's first page.
         * Found following hack online.
         */
        Slice<Stock> slice = stockRepository.findAll(CassandraPageRequest.first(pageSize));

        int currentPage = 0;
        while (slice.hasNext() && currentPage < pageNo) {
            slice = stockRepository.findAll(slice.nextPageable());
            currentPage++;
        }

        return slice;
    }


    private StockResponse buildStockResponse(Stock stock) {

        return StockResponse.builder()
                .id(stock.getId().toString())
                .name(stock.getName())
                .currentPrice(stock.getCurrentPrice())
                .lastUpdate(stock.getLastUpdate())
                .build();
    }

    public StockResponse getStock(String id) {

        Optional<Stock> existingStock = stockRepository.findById(id);

        if (!existingStock.isPresent()) {
            throw new StockNotFoundException("Stock [id: " + id + "] not found.");
        }

        return buildStockResponse(existingStock.get());
    }

    public StockResponse updateStock(UpdateStockRequest updateStockRequest) {

        Optional<Stock> existingStock = stockRepository.findById(updateStockRequest.getId());

        if (!existingStock.isPresent()) {
            throw new StockNotFoundException("Stock [id: " + updateStockRequest.getId() + "] not found.");
        }

        Stock stock = existingStock.get();
        stock.setName(updateStockRequest.getName());
        stock.setCurrentPrice(updateStockRequest.getCurrentPrice());
        stock.setLastUpdate(dateTimeUtil.getCurrentTimeEpochMilli());

        stockRepository.save(stock);

        return buildStockResponse(stock);
    }

    public IdentityResponse createStock(CreateStockRequest createStockRequest) {

        Stock stock = Stock.builder()
                .id(UUID.fromString(uuidUtil.getRandomUuid()))
                .name(createStockRequest.getName())
                .currentPrice(createStockRequest.getCurrentPrice())
                .lastUpdate(dateTimeUtil.getCurrentTimeEpochMilli())
                .build();

        stockRepository.save(stock);

        return IdentityResponse.builder().id(stock.getId().toString()).build();
    }
}
