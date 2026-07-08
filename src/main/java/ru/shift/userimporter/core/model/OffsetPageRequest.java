package ru.shift.userimporter.core.model;


import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record OffsetPageRequest(long offset, int limit) implements Pageable {
    public OffsetPageRequest {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be >= 0");
        }

        if (limit < 1) {
            throw new IllegalArgumentException("Limit must >= 1");
        }
    }

    @Override
    public int getPageNumber() {
        return (int) (offset / limit);
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return Sort.unsorted();
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }

    @Override
    public Pageable next() {
        return new OffsetPageRequest(offset + limit, limit);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? new OffsetPageRequest(offset - limit, limit) : first();
    }

    @Override
    public Pageable first() {
        return new OffsetPageRequest(0, limit);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetPageRequest((long) pageNumber * limit, limit);
    }
}
