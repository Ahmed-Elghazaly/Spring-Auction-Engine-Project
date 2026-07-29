package com.bidforge.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


class PagingTest {

    private static final Set<String> ALLOWED = Set.of("createdAt", "endTime", "startingPrice");


    @Test
    void clampsPageSizeToTheMaximum() {
        Pageable pageable = Paging.of(0, 1_000_000, null, ALLOWED);

        assertThat(pageable.getPageSize())
                .as("a client must not be able to request an unbounded page")
                .isEqualTo(50);
    }

    @Test
    void clampsPageSizeAtExactlyTheBoundary() {
        assertThat(Paging.of(0, 50, null, ALLOWED).getPageSize()).isEqualTo(50);
        assertThat(Paging.of(0, 51, null, ALLOWED).getPageSize()).isEqualTo(50);
        assertThat(Paging.of(0, 49, null, ALLOWED).getPageSize()).isEqualTo(49);
    }

    @Test
    void raisesZeroOrNegativePageSizeToOne() {
        // PageRequest itself rejects a size below 1, so the floor prevents a crash.
        assertThat(Paging.of(0, 0, null, ALLOWED).getPageSize()).isEqualTo(1);
        assertThat(Paging.of(0, -10, null, ALLOWED).getPageSize()).isEqualTo(1);
    }

    @Test
    void keepsAReasonablePageSizeUntouched() {
        assertThat(Paging.of(0, 12, null, ALLOWED).getPageSize()).isEqualTo(12);
    }



    @Test
    void raisesNegativePageNumberToZero() {
        assertThat(Paging.of(-5, 20, null, ALLOWED).getPageNumber()).isZero();
    }



    @Test
    void defaultsToNewestFirstWhenNoSortIsGiven() {
        Sort sort = Paging.of(0, 20, null, ALLOWED).getSort();

        assertThat(sort.getOrderFor("createdAt")).isNotNull();
        assertThat(sort.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void honoursAWhitelistedSortField() {
        Sort sort = Paging.of(0, 20, "endTime,asc", ALLOWED).getSort();

        assertThat(sort.getOrderFor("endTime")).isNotNull();
        assertThat(sort.getOrderFor("endTime").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void defaultsToDescendingWhenNoDirectionIsGiven() {
        Sort sort = Paging.of(0, 20, "startingPrice", ALLOWED).getSort();

        assertThat(sort.getOrderFor("startingPrice").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void ignoresASortFieldThatIsNotWhitelisted() {
        Sort sort = Paging.of(0, 20, "password,asc", ALLOWED).getSort();

        assertThat(sort.getOrderFor("password"))
                .as("a non-whitelisted field must be ignored, not passed to SQL")
                .isNull();
        assertThat(sort.getOrderFor("createdAt"))
                .as("and the safe default is used instead")
                .isNotNull();
    }

    @Test
    void ignoresGarbageSortInput() {
        assertThat(Paging.of(0, 20, "   ", ALLOWED).getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(Paging.of(0, 20, ",,,", ALLOWED).getSort().getOrderFor("createdAt")).isNotNull();
    }

    @Test
    void supportsACustomDefaultSortFieldForEntitiesWithoutCreatedAt() {
        Sort sort = Paging.of(0, 20, null, Set.of("closedAt", "finalPrice"), "closedAt").getSort();

        assertThat(sort.getOrderFor("closedAt")).isNotNull();
        assertThat(sort.getOrderFor("closedAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }
}
