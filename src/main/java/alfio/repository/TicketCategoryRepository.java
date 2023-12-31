/**
 * This file is part of alf.io.
 *
 * alf.io is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * alf.io is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with alf.io.  If not, see <http://www.gnu.org/licenses/>.
 */
package alfio.repository;

import alfio.model.EntityIdAndMetadata;
import alfio.model.TicketCategory;
import alfio.model.TicketCategoryStatisticView;
import alfio.model.metadata.AlfioMetadata;
import alfio.model.support.JSONData;
import ch.digitalfondue.npjt.*;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@QueryRepository
public interface TicketCategoryRepository {

    String CHECK_ACTIVE = "event_id = :eventId and tc_status = 'ACTIVE'";

    @Query("insert into ticket_category(inception, expiration, name, max_tickets, price_cts, src_price_cts, access_restricted, tc_status, event_id, bounded, category_code, valid_checkin_from, valid_checkin_to, ticket_validity_start, ticket_validity_end, ordinal, ticket_checkin_strategy, metadata, ticket_access_type) " +
            "values(:inception, :expiration, :name, :max_tickets, 0, :price, :accessRestricted, 'ACTIVE', :eventId, :bounded, :code, :validCheckInFrom, :validCheckInTo, :ticketValidityStart, :ticketValidityEnd, :ordinal, :ticketCheckInStrategy, :metadata::jsonb, :ticketAccessType::ticket_access_type)")
    @AutoGeneratedKey("id")
    AffectedRowCountAndKey<Integer> insert(@Bind("inception") ZonedDateTime inception,
                                           @Bind("expiration") ZonedDateTime expiration,
                                           @Bind("name") String name,
                                           @Bind("max_tickets") int maxTickets,
                                           @Bind("accessRestricted") boolean accessRestricted,
                                           @Bind("eventId") int eventId,
                                           @Bind("bounded") boolean bounded,
                                           @Bind("price") int price,
                                           @Bind("code") String code,
                                           @Bind("validCheckInFrom") ZonedDateTime validCheckInFrom,
                                           @Bind("validCheckInTo") ZonedDateTime validCheckInTo,
                                           @Bind("ticketValidityStart") ZonedDateTime ticketValidityStart,
                                           @Bind("ticketValidityEnd") ZonedDateTime ticketValidityEnd,
                                           @Bind("ordinal") int ordinal,
                                           @Bind("ticketCheckInStrategy") TicketCategory.TicketCheckInStrategy ticketCheckInStrategy,
                                           @Bind("metadata") @JSONData AlfioMetadata metadata,
                                           @Bind("ticketAccessType") TicketCategory.TicketAccessType ticketAccessType);

    @Query("select * from ticket_category_with_currency where id = :id and " + CHECK_ACTIVE)
    TicketCategory getByIdAndActive(@Bind("id") int id, @Bind("eventId") int eventId);

    @Query("select exists(select id from ticket_category where id = :id and " + CHECK_ACTIVE + ")")
    Boolean checkCategoryExistsForEvent(@Bind("id") int id, @Bind("eventId") int eventId);

    @Query("select count(*) from ticket_category where id in (:ids) and " + CHECK_ACTIVE)
    Integer countCategoriesBelongingToEvent(@Bind("eventId") int eventId, @Bind("ids") Collection<Integer> categoryIds);

    @Query("select * from ticket_category_with_currency where id in (:ids) and " + CHECK_ACTIVE)
    List<TicketCategory> getByIdsAndActive(@Bind("ids") Collection<Integer> ids, @Bind("eventId") int eventId);

    @Query("select * from ticket_category_with_currency where id = :id and " + CHECK_ACTIVE)
    Optional<TicketCategory> getOptionalByIdAndActive(@Bind("id") int id, @Bind("eventId") int eventId);

    @Query("select * from ticket_category_with_currency where id = :id and tc_status = 'ACTIVE'")
    Optional<TicketCategory> getByIdAndActive(@Bind("id") int id);

    @Query("select * from ticket_category_with_currency where id = :id")
    TicketCategory getById(@Bind("id") int id);

    @Query("select * from ticket_category_with_currency where id in(:ids)")
    List<TicketCategory> findByIds(@Bind("ids") Collection<Integer> ids);

    @Query("select * from ticket_category_with_currency where event_id = :eventId and category_code = :code and tc_status = 'ACTIVE'")
    Optional<TicketCategory> findCodeInEvent(@Bind("eventId") int eventId, @Bind("code") String code);

    @Query("select count(*) from ticket_category_with_currency where " + CHECK_ACTIVE + " and bounded = false")
    Integer countUnboundedCategoriesByEventId(@Bind("eventId") int eventId);

    @Query("select * from ticket_category_with_currency where " + CHECK_ACTIVE + " and bounded = false order by expiration desc")
    List<TicketCategory> findUnboundedOrderByExpirationDesc(@Bind("eventId") int eventId);

    @Query("select * from ticket_category_with_currency where event_id = :eventId  and tc_status = 'ACTIVE' order by ordinal asc, inception asc, expiration asc, id asc")
    List<TicketCategory> findAllTicketCategories(@Bind("eventId") int eventId);

    @Query("select tc.* from ticket_category_with_currency tc" +
        "    join ticket_category_statistics tcs on tc.id = tcs.ticket_category_id" +
        "    where tc.event_id = :eventId" +
        "    and tcs.is_expired is FALSE" +
        "    and tcs.access_restricted is FALSE" +
        "    and (tcs.bounded is FALSE or tcs.not_sold_tickets > 0)" +
        " order by tc.inception, tc.expiration, tc.id" +
        " limit 1")
    Optional<TicketCategory> findFirstWithAvailableTickets(@Bind("eventId") int eventId);

    default Map<Integer, TicketCategory> findByEventIdAsMap(int eventId) {
        return findAllTicketCategories(eventId).stream().collect(Collectors.toMap(TicketCategory::getId, Function.identity()));
    }

    @Query("select id, metadata from ticket_category where " + CHECK_ACTIVE + " order by ordinal asc, inception asc, expiration asc, id asc")
    List<EntityIdAndMetadata> findMetadataForCategoriesInEvent(@Bind("eventId") int eventId);

    default Map<Integer, AlfioMetadata> findCategoryMetadataForEventGroupByCategoryId(int eventId) {
        return findMetadataForCategoriesInEvent(eventId).stream()
            .filter(ei -> ei.getMetadata() != null)
            .collect(Collectors.toMap(EntityIdAndMetadata::getId, EntityIdAndMetadata::getMetadata));
    }
    
    @Query("select count(*) from ticket_category_with_currency where event_id = :eventId and access_restricted = true")
    Integer countAccessRestrictedRepositoryByEventId(@Bind("eventId") int eventId);

    @Query("update ticket_category set name = :name, inception = :inception, expiration = :expiration, " +
            "max_tickets = :max_tickets, src_price_cts = :price, access_restricted = :accessRestricted, category_code = :code, " +
            " valid_checkin_from = :validCheckInFrom, valid_checkin_to = :validCheckInTo, ticket_validity_start = :ticketValidityStart," +
            " ticket_validity_end = :ticketValidityEnd, ticket_checkin_strategy = :ticketCheckInStrategy, ticket_access_type = :ticketAccessType::ticket_access_type where id = :id")
    int update(@Bind("id") int id,
               @Bind("name") String name,
               @Bind("inception") ZonedDateTime inception,
               @Bind("expiration") ZonedDateTime expiration,
               @Bind("max_tickets") int maxTickets,
               @Bind("accessRestricted") boolean accessRestricted,
               @Bind("price") int price,
               @Bind("code") String code,
               @Bind("validCheckInFrom") ZonedDateTime validCheckInFrom,
               @Bind("validCheckInTo") ZonedDateTime validCheckInTo,
               @Bind("ticketValidityStart") ZonedDateTime ticketValidityStart,
               @Bind("ticketValidityEnd") ZonedDateTime ticketValidityEnd,
               @Bind("ticketCheckInStrategy") TicketCategory.TicketCheckInStrategy ticketCheckInStrategy,
               @Bind("ticketAccessType") TicketCategory.TicketAccessType ticketAccessType);

    @Query("update ticket_category set max_tickets = :max_tickets where id = :id")
    int updateSeatsAvailability(@Bind("id") int id, @Bind("max_tickets") int maxTickets);

    @Query("update ticket_category set bounded = :bounded where id = :id")
    int updateBoundedFlag(@Bind("id") int id, @Bind("bounded") boolean bounded);

    @Query("update ticket_category set inception = :inception, expiration = :expiration where id = :id")
    int fixDates(@Bind("id") int id, @Bind("inception") ZonedDateTime inception, @Bind("expiration") ZonedDateTime expiration);

    default int getTicketAllocation(int eventId) {
        return findAllTicketCategories(eventId).stream()
            .filter(TicketCategory::isBounded)
            .mapToInt(TicketCategory::getMaxTickets)
            .sum();
    }

    @Query("select * from ticket_category_statistics where ticket_category_id = :ticketCategoryId and event_id = :eventId")
    TicketCategoryStatisticView findStatisticWithId(@Bind("ticketCategoryId") int ticketCategoryId, @Bind("eventId") int eventId);

    @Query("select * from ticket_category_statistics where event_id = :eventId")
    List<TicketCategoryStatisticView> findStatisticsForEventId(@Bind("eventId") int eventId);

    default Map<Integer, TicketCategoryStatisticView> findStatisticsForEventIdByCategoryId(int eventId) {
        return findStatisticsForEventId(eventId).stream().collect(Collectors.toMap(TicketCategoryStatisticView::getId, Function.identity()));
    }

    @Query("select access_restricted from ticket_category where id = :id")
    Boolean isAccessRestricted(@Bind("id") Integer hiddenCategoryId);

    @Query("update ticket_category set tc_status = 'NOT_ACTIVE' from (select count(*) cnt from ticket where category_id = :categoryId and status in ('PENDING', 'ACQUIRED', 'CHECKED_IN')) tkts where id = :categoryId and tkts.cnt = 0")
    int deleteCategoryIfEmpty(@Bind("categoryId") int categoryId);

    @Query(type = QueryType.TEMPLATE, value = "update ticket_category set ordinal = :ordinal where id = :id and event_id = :eventId")
    String updateOrdinal();

    @Query("select ticket_checkin_strategy from ticket_category where id = :id")
    TicketCategory.TicketCheckInStrategy getCheckInStrategy(@Bind("id") int categoryId);

    @Query("select count(*) from ticket_category where id in (:categoryIds) and src_price_cts > 0")
    Integer countPaidCategoriesInReservation(@Bind("categoryIds") Collection<Integer> categoryIds);

    @Query("select distinct tc.* from ticket_category_with_currency tc join ticket t on t.category_id = tc.id where t.tickets_reservation_id = :reservationId")
    List<TicketCategory> findCategoriesInReservation(@Bind("reservationId") String reservationId);

    @JSONData
    @Query("select metadata from ticket_category where id = :id and event_id = :eventId")
    AlfioMetadata getMetadata(@Bind("eventId") int eventId, @Bind("id") int categoryId);

    @Query("update ticket_category set metadata = :metadata::jsonb where id = :id and event_id = :eventId")
    int updateMetadata(@Bind("metadata") @JSONData AlfioMetadata metadata, @Bind("eventId") int eventId, @Bind("id") int categoryId);

    @Query("update ticket_category set ticket_access_type = :ticketAccessType::ticket_access_type where event_id = :eventId")
    void updateTicketAccessTypeForEvent(@Bind("eventId") int eventId, @Bind("ticketAccessType") TicketCategory.TicketAccessType ticketAccessType);

    @Query("select count(*) from ticket_category where " + CHECK_ACTIVE)
    Integer countActiveByEventId(@Bind("eventId") int eventId);

    @Query("select count(*) from ticket_category where event_id = :eventId and id in (:categoryIds)")
    int countCategoryForEvent(@Bind("categoryIds") Set<Integer> categoryIds, @Bind("eventId") int eventId);
}
