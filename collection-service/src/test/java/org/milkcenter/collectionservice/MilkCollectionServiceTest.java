package org.milkcenter.collectionservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.milkcenter.collectionservice.enums.CollectionStatus;
import org.milkcenter.collectionservice.repository.MilkCollectionRepository;
import org.milkcenter.collectionservice.security.CurrentUserService;
import org.milkcenter.collectionservice.service.MilkCollectionService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MilkCollectionServiceTest {

    @Mock
    private MilkCollectionRepository collectionRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private MilkCollectionService service;

    @Test
    void monthlyAcceptedLitersReturnsRepositoryTotal() {
        when(collectionRepository
                .sumQuantityByFarmerIdAndStatusAndPeriod(
                        any(Long.class),
                        any(CollectionStatus.class),
                        any(Date.class),
                        any(Date.class)))
                .thenReturn(new BigDecimal("4850.500"));

        BigDecimal total = service.getMonthlyAcceptedLiters(
                15L,
                9,
                2026
        );

        assertEquals(new BigDecimal("4850.500"), total);
        verify(collectionRepository)
                .sumQuantityByFarmerIdAndStatusAndPeriod(
                        eq(15L),
                        eq(CollectionStatus.ACCEPTED),
                        any(Date.class),
                        any(Date.class)
                );
    }

    @Test
    void monthlyAcceptedLitersReturnsZeroWhenNoCollectionExists() {
        when(collectionRepository
                .sumQuantityByFarmerIdAndStatusAndPeriod(
                        any(Long.class),
                        any(CollectionStatus.class),
                        any(Date.class),
                        any(Date.class)))
                .thenReturn(null);

        BigDecimal total = service.getMonthlyAcceptedLiters(
                15L,
                9,
                2026
        );

        assertEquals(BigDecimal.ZERO, total);
    }

    @Test
    void monthlyAcceptedLitersRejectsInvalidMonth() {
        assertThrows(ResponseStatusException.class,
                () -> service.getMonthlyAcceptedLiters(15L, 13, 2026));
    }

    @Test
    void monthlyAcceptedLitersRejectsNullFarmer() {
        assertThrows(ResponseStatusException.class,
                () -> service.getMonthlyAcceptedLiters(null, 9, 2026));
    }
}
