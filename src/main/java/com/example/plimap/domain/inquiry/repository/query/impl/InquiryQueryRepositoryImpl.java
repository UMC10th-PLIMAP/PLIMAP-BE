package com.example.plimap.domain.inquiry.repository.query.impl;

import com.example.plimap.domain.inquiry.dto.CursorInfo;
import com.example.plimap.domain.inquiry.dto.Pagination;
import com.example.plimap.domain.inquiry.entity.Inquiry;
import com.example.plimap.domain.inquiry.entity.QInquiry;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import com.example.plimap.domain.inquiry.exception.InquiryException;
import com.example.plimap.domain.inquiry.repository.query.InquiryQueryRepository;
import com.example.plimap.domain.member.entity.QMember;
import com.example.plimap.global.apiPayload.code.GeneralErrorCode;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InquiryQueryRepositoryImpl implements InquiryQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Pagination<Inquiry> findInquiries(InquiryCategory category, String cursor, Integer pageSize) {
        QInquiry inquiry = QInquiry.inquiry;
        QMember member = QMember.member;
        CursorInfo cursorInfo = parseCursor(cursor);

        List<Inquiry> data = queryFactory
                .selectFrom(inquiry)
                .leftJoin(inquiry.member, member).fetchJoin()
                .where(
                        categoryCondition(inquiry, category),
                        cursorCondition(inquiry, cursorInfo)
                )
                .orderBy(inquiry.createdAt.desc(), inquiry.id.desc())
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = data.size() > pageSize;
        if (hasNext) {
            data.remove(pageSize.intValue());
        }

        if (data.isEmpty()) {
            return toPagination(data, null, false, pageSize);
        }

        Inquiry last = data.get(data.size() - 1);
        String nextCursor = hasNext
                ? last.getCreatedAt() + "/" + last.getId()
                : null;

        return toPagination(data, nextCursor, hasNext, pageSize);
    }

    private BooleanExpression categoryCondition(QInquiry inquiry, InquiryCategory category) {
        return category != null ? inquiry.category.eq(category) : null;
    }

    private BooleanExpression cursorCondition(QInquiry inquiry, CursorInfo cursorInfo) {
        if (cursorInfo.createdAt() == null || cursorInfo.id() == null) {
            return null;
        }
        return inquiry.createdAt.lt(cursorInfo.createdAt())
                .or(
                        inquiry.createdAt.eq(cursorInfo.createdAt())
                                .and(inquiry.id.lt(cursorInfo.id()))
                );
    }

    private CursorInfo parseCursor(String cursor) {
        if (cursor == null) {
            return new CursorInfo(null, null);
        }
        try {
            String[] parts = cursor.split("/");
            if (parts.length != 2) {
                throw new InquiryException(GeneralErrorCode.INVALID_CURSOR);
            }

            Instant createdAt = Instant.parse(parts[0]);
            long id = Long.parseLong(parts[1]);

            return new CursorInfo(createdAt, id);
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new InquiryException(GeneralErrorCode.INVALID_CURSOR, e);
        }
    }

    private Pagination<Inquiry> toPagination(List<Inquiry> data, String nextCursor, Boolean hasNext, Integer pageSize) {
        return Pagination.<Inquiry>builder()
                .data(data)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .pageSize(pageSize)
                .build();
    }
}
