package com.worldcup.hotelbooking.catalog.query.hotel;

import com.worldcup.hotelbooking.availability_pricing.pricing.EnhancedPricingServiceImpl;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.hotelphoto.HotelPhotoRepository;
import com.worldcup.hotelbooking.catalog.hotelphoto.dto.HotelPrimaryPhotoProjection;
import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogResponseDto;
import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogSearchMode;
import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogSearchResponseDto;
import com.worldcup.hotelbooking.catalog.query.hotel.exception.CheckOutBeforeCheckIn;
import com.worldcup.hotelbooking.catalog.query.hotel.exception.CheckOutDateAreRequired;
import com.worldcup.hotelbooking.catalog.query.hotel.mapper.HotelCatalogMapper;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.storage.PhotoUrlResolver;
import com.worldcup.hotelbooking.tournament.match.Match;
import com.worldcup.hotelbooking.tournament.match.MatchRepository;
import com.worldcup.hotelbooking.tournament.stadium.Stadium;
import com.worldcup.hotelbooking.tournament.stadium.StadiumRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class HotelCatalogServiceImplTest {

    private HotelRepository hotelRepository;
    private EnhancedPricingServiceImpl enhancedPricingService;
    private HotelCatalogMapper hotelCatalogMapper;
    private HotelPhotoRepository hotelPhotoRepository;
    private PhotoUrlResolver photoUrlResolver;
    private MatchRepository matchRepository;
    private StadiumRepository stadiumRepository;
    private HotelCatalogServiceImpl service;

    @BeforeEach
    void setUp() {
        hotelRepository = mock(HotelRepository.class);
        enhancedPricingService = mock(EnhancedPricingServiceImpl.class);
        hotelCatalogMapper = mock(HotelCatalogMapper.class);
        hotelPhotoRepository = mock(HotelPhotoRepository.class);
        photoUrlResolver = mock(PhotoUrlResolver.class);
        matchRepository = mock(MatchRepository.class);
        stadiumRepository = mock(StadiumRepository.class);

        service = new HotelCatalogServiceImpl(
                hotelRepository,
                enhancedPricingService,
                hotelCatalogMapper,
                hotelPhotoRepository,
                photoUrlResolver,
                matchRepository,
                stadiumRepository
        );
    }

    private Hotel buildHotel(Long id, String name, String city, Double lat, Double lon) {
        Hotel hotel = new Hotel();
        hotel.setId(id);
        hotel.setName(name);
        hotel.setDescription("desc-" + id);
        hotel.setCity(city);
        hotel.setCountry("Palestine");
        hotel.setLatitude(lat);
        hotel.setLongitude(lon);
        hotel.setAverageRating(BigDecimal.valueOf(4.2));
        hotel.setReviewCount(20);

        hotel.setHasGym(true);
        hotel.setHasWifi(true);
        hotel.setHasParking(true);
        hotel.setHasBreakfast(true);
        hotel.setHasAirConditioning(true);
        hotel.setHasHeating(true);
        hotel.setHasPool(false);
        hotel.setHasSpa(false);
        hotel.setHasElevator(true);
        hotel.setHasRestaurant(true);
        hotel.setHasRoomService(true);
        hotel.setHasLaundry(true);
        hotel.setHasAirportShuttle(false);
        hotel.setHasAccessibleFacilities(true);
        hotel.setPetFriendly(false);

        return hotel;
    }

    private RoomType buildRoomType(Long id, BigDecimal basePrice) {
        RoomType roomType = new RoomType();
        roomType.setId(id);
        roomType.setName("Room-" + id);
        roomType.setBasePrice(basePrice);
        roomType.setTotalRooms(5);
        roomType.setMaxAdults(2);
        roomType.setMaxChildren(2);
        return roomType;
    }

    private Stadium buildStadium(Long id, String city, Double lat, Double lon) {
        Stadium stadium = new Stadium();
        stadium.setId(id);
        stadium.setCity(city);
        stadium.setLatitude(lat);
        stadium.setLongitude(lon);
        return stadium;
    }

    private Match buildMatch(Long id, Stadium stadium) {
        Match match = new Match();
        match.setId(id);
        match.setStadium(stadium);
        return match;
    }

    private HotelCatalogResponseDto dto(
            Long id,
            String name,
            String city,
            BigDecimal minPrice,
            Double distanceKm
    ) {
        return new HotelCatalogResponseDto(
                name,
                id,
                "desc-" + id,
                city,
                "Palestine",
                "url" + id,
                BigDecimal.valueOf(4.2),
                20,
                minPrice,
                distanceKm,
                true,
                true,
                true,
                true,
                true,
                true,
                false,
                false,
                true,
                true,
                true,
                true,
                false,
                true,
                false
        );
    }

    private void mockPrimaryPhoto(Long hotelId, String storageKey, String url) {
        given(hotelPhotoRepository.findPrimaryPhotosByHotelIds(List.of(hotelId)))
                .willReturn(List.of(new HotelPrimaryPhotoProjection(hotelId, storageKey)));
        given(photoUrlResolver.resolve(storageKey)).willReturn(url);
    }

    private void mockPrimaryPhotos(List<Long> hotelIds, List<HotelPrimaryPhotoProjection> projections) {
        given(hotelPhotoRepository.findPrimaryPhotosByHotelIds(hotelIds)).willReturn(projections);
        for (HotelPrimaryPhotoProjection projection : projections) {
            given(photoUrlResolver.resolve(projection.storageKey()))
                    .willReturn("url" + projection.hotelId());
        }
    }

    private void mockMapper(Hotel hotel, String url, HotelCatalogResponseDto dto) {
        given(hotelCatalogMapper.toDto(
                eq(hotel),
                eq(url),
                any(),
                any()
        )).willReturn(dto);
    }

    private void stubComputedFindAll(List<Hotel> hotels) {
        given(hotelRepository.findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                argThat((Pageable p) -> p.getPageNumber() == 0 && p.getPageSize() == 500)
        )).willReturn(new PageImpl<>(hotels, PageRequest.of(0, 500), hotels.size()));
    }

    private void stubComputedFindAllSequence(PageImpl<Hotel>... pages) {
        given(hotelRepository.findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                argThat((Pageable p) -> p.getPageNumber() == 0 && p.getPageSize() == 500)
        )).willReturn(pages[0], java.util.Arrays.copyOfRange(pages, 1, pages.length));
    }

    @Test
    @DisplayName("search -> should return normal response when no computed filter or sort exists")
    void search_WithoutComputedFiltersOrSort_ShouldUseDatabasePaging() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name").ascending());
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();

        Hotel h1 = buildHotel(1L, "Royal", "Nablus", 32.22, 35.26);
        Hotel h2 = buildHotel(2L, "Sea View", "Gaza", 31.50, 34.46);

        RoomType rt1 = buildRoomType(11L, BigDecimal.valueOf(150));
        RoomType rt2 = buildRoomType(22L, BigDecimal.valueOf(200));
        h1.setRoomTypes(List.of(rt1));
        h2.setRoomTypes(List.of(rt2));

        HotelCatalogResponseDto dto1 = dto(1L, "Royal", "Nablus", BigDecimal.valueOf(150), null);
        HotelCatalogResponseDto dto2 = dto(2L, "Sea View", "Gaza", BigDecimal.valueOf(200), null);

        given(hotelRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(h1, h2), pageable, 2));

        given(hotelPhotoRepository.findPrimaryPhotosByHotelIds(List.of(1L, 2L)))
                .willReturn(List.of(
                        new HotelPrimaryPhotoProjection(1L, "hotels/1.jpg"),
                        new HotelPrimaryPhotoProjection(2L, "hotels/2.jpg")
                ));

        given(photoUrlResolver.resolve("hotels/1.jpg")).willReturn("url1");
        given(photoUrlResolver.resolve("hotels/2.jpg")).willReturn("url2");

        mockMapper(h1, "url1", dto1);
        mockMapper(h2, "url2", dto2);

        HotelCatalogSearchResponseDto result = service.search(pageable, criteria);

        assertEquals(HotelCatalogSearchMode.NORMAL, result.getSearchMode());
        assertFalse(result.isFallbackApplied());
        assertEquals("Catalog retrieved successfully", result.getMessage());
        assertEquals(2, result.getHotels().getTotalElements());
        assertEquals(2, result.getHotels().getContent().size());
        assertEquals("Royal", result.getHotels().getContent().get(0).getName());
        assertEquals("Sea View", result.getHotels().getContent().get(1).getName());
    }

    @Test
    @DisplayName("search -> should throw when sort field is invalid")
    void search_WithInvalidSortField_ShouldThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("unknown"));
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.search(pageable, criteria)
        );

        assertTrue(ex.getMessage().contains("Invalid sort field"));
    }

    @Test
    @DisplayName("search -> should throw when distance filter exists without coordinates")
    void search_WithDistanceFilterWithoutCoordinates_ShouldThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name"));
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setMaxDistanceKm(10.0);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.search(pageable, criteria)
        );

        assertTrue(ex.getMessage().contains("latitude and longitude are required"));
    }

    @Test
    @DisplayName("search -> should throw when sorting by distance without coordinates")
    void search_SortingByDistanceWithoutCoordinates_ShouldThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("distance"));
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.search(pageable, criteria)
        );

        assertTrue(ex.getMessage().contains("sorting by distance"));
    }

    @Test
    @DisplayName("search -> should throw when price filter exists without dates")
    void search_WithPriceFilterWithoutDates_ShouldThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name"));
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setMinTotalPrice(BigDecimal.valueOf(100));

        assertThrows(CheckOutDateAreRequired.class,
                () -> service.search(pageable, criteria));
    }

    @Test
    @DisplayName("search -> should throw when sorting by price without dates")
    void search_SortingByPriceWithoutDates_ShouldThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price"));
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();

        assertThrows(CheckOutDateAreRequired.class,
                () -> service.search(pageable, criteria));
    }

    @Test
    @DisplayName("search -> should throw when checkOut is not after checkIn")
    void search_WithInvalidDateRange_ShouldThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price"));
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setCheckInDate(LocalDate.of(2026, 3, 20));
        criteria.setCheckOutDate(LocalDate.of(2026, 3, 20));

        assertThrows(CheckOutBeforeCheckIn.class,
                () -> service.search(pageable, criteria));
    }

    @Test
    @DisplayName("search -> should filter hotels by computed price range")
    void search_WithPriceFilter_ShouldFilterHotelsByComputedMinPrice() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name"));
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setMinTotalPrice(BigDecimal.valueOf(200));
        criteria.setMaxTotalPrice(BigDecimal.valueOf(400));
        criteria.setCheckInDate(LocalDate.of(2026, 3, 20));
        criteria.setCheckOutDate(LocalDate.of(2026, 3, 22));
        criteria.setNumberOfRooms(1);

        Hotel h1 = buildHotel(1L, "Royal", "Nablus", 32.22, 35.26);
        Hotel h2 = buildHotel(2L, "Sea View", "Gaza", 31.50, 34.46);

        RoomType rt1 = buildRoomType(11L, BigDecimal.valueOf(150));
        RoomType rt2 = buildRoomType(12L, BigDecimal.valueOf(250));
        RoomType rt3 = buildRoomType(21L, BigDecimal.valueOf(600));

        h1.setRoomTypes(List.of(rt1, rt2));
        h2.setRoomTypes(List.of(rt3));

        stubComputedFindAll(List.of(h1, h2));

        given(enhancedPricingService.calculateTotalStayPrice(
                criteria.getCheckInDate(), criteria.getCheckOutDate(), h1, rt1, 1))
                .willReturn(BigDecimal.valueOf(300));
        given(enhancedPricingService.calculateTotalStayPrice(
                criteria.getCheckInDate(), criteria.getCheckOutDate(), h1, rt2, 1))
                .willReturn(BigDecimal.valueOf(350));
        given(enhancedPricingService.calculateTotalStayPrice(
                criteria.getCheckInDate(), criteria.getCheckOutDate(), h2, rt3, 1))
                .willReturn(BigDecimal.valueOf(600));

        mockPrimaryPhoto(1L, "hotels/1.jpg", "url1");

        HotelCatalogResponseDto mapped = dto(1L, "Royal", "Nablus", BigDecimal.valueOf(300), null);
        mockMapper(h1, "url1", mapped);

        HotelCatalogSearchResponseDto result = service.search(pageable, criteria);

        assertEquals(HotelCatalogSearchMode.NORMAL, result.getSearchMode());
        assertEquals(1, result.getHotels().getTotalElements());
        assertEquals(1, result.getHotels().getContent().size());
        assertEquals("Royal", result.getHotels().getContent().get(0).getName());
        assertEquals(BigDecimal.valueOf(300), result.getHotels().getContent().get(0).getMinPrice());
    }

    @Test
    @DisplayName("search -> should sort by price ascending")
    void search_SortingByPriceAscending_ShouldSortByComputedMinPrice() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setCheckInDate(LocalDate.of(2026, 3, 20));
        criteria.setCheckOutDate(LocalDate.of(2026, 3, 22));
        criteria.setNumberOfRooms(1);

        Hotel h1 = buildHotel(1L, "Hotel A", "Nablus", 32.22, 35.26);
        Hotel h2 = buildHotel(2L, "Hotel B", "Gaza", 31.50, 34.46);

        RoomType rt1 = buildRoomType(11L, BigDecimal.valueOf(500));
        RoomType rt2 = buildRoomType(22L, BigDecimal.valueOf(200));

        h1.setRoomTypes(List.of(rt1));
        h2.setRoomTypes(List.of(rt2));

        stubComputedFindAll(List.of(h1, h2));

        given(enhancedPricingService.calculateTotalStayPrice(
                criteria.getCheckInDate(), criteria.getCheckOutDate(), h1, rt1, 1))
                .willReturn(BigDecimal.valueOf(500));
        given(enhancedPricingService.calculateTotalStayPrice(
                criteria.getCheckInDate(), criteria.getCheckOutDate(), h2, rt2, 1))
                .willReturn(BigDecimal.valueOf(200));

        given(hotelPhotoRepository.findPrimaryPhotosByHotelIds(List.of(2L, 1L)))
                .willReturn(List.of(
                        new HotelPrimaryPhotoProjection(2L, "hotels/2.jpg"),
                        new HotelPrimaryPhotoProjection(1L, "hotels/1.jpg")
                ));

        given(photoUrlResolver.resolve("hotels/2.jpg")).willReturn("url2");
        given(photoUrlResolver.resolve("hotels/1.jpg")).willReturn("url1");

        HotelCatalogResponseDto dto2 = dto(2L, "Hotel B", "Gaza", BigDecimal.valueOf(200), null);
        HotelCatalogResponseDto dto1 = dto(1L, "Hotel A", "Nablus", BigDecimal.valueOf(500), null);

        mockMapper(h2, "url2", dto2);
        mockMapper(h1, "url1", dto1);

        HotelCatalogSearchResponseDto result = service.search(pageable, criteria);

        assertEquals(2, result.getHotels().getContent().size());
        assertEquals("Hotel B", result.getHotels().getContent().get(0).getName());
        assertEquals("Hotel A", result.getHotels().getContent().get(1).getName());
    }

    @Test
    @DisplayName("search -> should sort by distance ascending")
    void search_SortingByDistanceAscending_ShouldSortByDistance() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("distance").ascending());
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setLatitude(32.22);
        criteria.setLongitude(35.26);

        Hotel nearHotel = buildHotel(1L, "Near Hotel", "Nablus", 32.221, 35.261);
        Hotel farHotel = buildHotel(2L, "Far Hotel", "Gaza", 31.50, 34.46);

        RoomType rt1 = buildRoomType(11L, BigDecimal.valueOf(150));
        RoomType rt2 = buildRoomType(22L, BigDecimal.valueOf(150));
        nearHotel.setRoomTypes(List.of(rt1));
        farHotel.setRoomTypes(List.of(rt2));

        stubComputedFindAll(List.of(nearHotel, farHotel));

        given(hotelPhotoRepository.findPrimaryPhotosByHotelIds(List.of(1L, 2L)))
                .willReturn(List.of(
                        new HotelPrimaryPhotoProjection(1L, "hotels/1.jpg"),
                        new HotelPrimaryPhotoProjection(2L, "hotels/2.jpg")
                ));

        given(photoUrlResolver.resolve("hotels/1.jpg")).willReturn("url1");
        given(photoUrlResolver.resolve("hotels/2.jpg")).willReturn("url2");

        HotelCatalogResponseDto dto1 = dto(1L, "Near Hotel", "Nablus", BigDecimal.valueOf(150), 0.15);
        HotelCatalogResponseDto dto2 = dto(2L, "Far Hotel", "Gaza", BigDecimal.valueOf(150), 90.0);

        mockMapper(nearHotel, "url1", dto1);
        mockMapper(farHotel, "url2", dto2);

        HotelCatalogSearchResponseDto result = service.search(pageable, criteria);

        assertEquals(2, result.getHotels().getContent().size());
        assertEquals("Near Hotel", result.getHotels().getContent().get(0).getName());
        assertEquals("Far Hotel", result.getHotels().getContent().get(1).getName());
        assertNotNull(result.getHotels().getContent().get(0).getDistanceKm());
    }

    @Test
    @DisplayName("search -> should slice computed result page correctly")
    void search_WithComputedSorting_ShouldSlicePage() {
        Pageable pageable = PageRequest.of(1, 1, Sort.by("price").ascending());
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setCheckInDate(LocalDate.of(2026, 3, 20));
        criteria.setCheckOutDate(LocalDate.of(2026, 3, 22));
        criteria.setNumberOfRooms(1);

        Hotel h1 = buildHotel(1L, "A Hotel", "Nablus", 32.22, 35.26);
        Hotel h2 = buildHotel(2L, "B Hotel", "Gaza", 31.50, 34.46);

        RoomType rt1 = buildRoomType(11L, BigDecimal.valueOf(100));
        RoomType rt2 = buildRoomType(22L, BigDecimal.valueOf(200));

        h1.setRoomTypes(List.of(rt1));
        h2.setRoomTypes(List.of(rt2));

        stubComputedFindAll(List.of(h1, h2));

        given(enhancedPricingService.calculateTotalStayPrice(
                criteria.getCheckInDate(), criteria.getCheckOutDate(), h1, rt1, 1))
                .willReturn(BigDecimal.valueOf(100));
        given(enhancedPricingService.calculateTotalStayPrice(
                criteria.getCheckInDate(), criteria.getCheckOutDate(), h2, rt2, 1))
                .willReturn(BigDecimal.valueOf(200));

        mockPrimaryPhoto(2L, "hotels/2.jpg", "url2");

        HotelCatalogResponseDto dto2 = dto(2L, "B Hotel", "Gaza", BigDecimal.valueOf(200), null);
        mockMapper(h2, "url2", dto2);

        HotelCatalogSearchResponseDto result = service.search(pageable, criteria);

        assertEquals(2, result.getHotels().getTotalElements());
        assertEquals(1, result.getHotels().getContent().size());
        assertEquals("B Hotel", result.getHotels().getContent().get(0).getName());
    }

    @Test
    @DisplayName("search -> should return empty page when repository returns no hotels")
    void search_WhenNoHotels_ShouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name"));
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();

        given(hotelRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        HotelCatalogSearchResponseDto result = service.search(pageable, criteria);

        assertEquals(0, result.getHotels().getTotalElements());
        assertTrue(result.getHotels().getContent().isEmpty());
        assertEquals(HotelCatalogSearchMode.NORMAL, result.getSearchMode());

        verify(hotelPhotoRepository, never()).findPrimaryPhotosByHotelIds(any());
    }

    @Test
    @DisplayName("search -> should use fallback radius mode when only matchId is provided and results exist")
    void search_WithMatchIdOnly_ShouldReturn5KmRadius() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setMatchId(100L);

        Stadium stadium = buildStadium(10L, "Nablus", 32.22, 35.26);
        Match match = buildMatch(100L, stadium);
        Hotel hotel = buildHotel(1L, "Nearby Hotel", "Nablus", 32.221, 35.261);

        RoomType rt = buildRoomType(11L, BigDecimal.valueOf(150));
        hotel.setRoomTypes(List.of(rt));

        given(matchRepository.findById(100L)).willReturn(Optional.of(match));

        // بما أن منطقك الحالي قد يكمل حتى 50 كم للوصول إلى حد أدنى من النتائج
        stubComputedFindAllSequence(
                new PageImpl<>(List.of(), PageRequest.of(0, 500), 0), // 5 km
                new PageImpl<>(List.of(), PageRequest.of(0, 500), 0), // 15 km
                new PageImpl<>(List.of(), PageRequest.of(0, 500), 0), // 30 km
                new PageImpl<>(List.of(hotel), PageRequest.of(0, 500), 1) // 50 km
        );

        mockPrimaryPhoto(1L, "hotels/1.jpg", "url1");

        HotelCatalogResponseDto mapped = dto(1L, "Nearby Hotel", "Nablus", BigDecimal.valueOf(150), 0.15);
        mockMapper(hotel, "url1", mapped);

        HotelCatalogSearchResponseDto result = service.search(pageable, criteria);

        assertEquals(HotelCatalogSearchMode.MATCH_RADIUS_50KM, result.getSearchMode());
        assertTrue(result.isFallbackApplied());
        assertEquals(1, result.getHotels().getTotalElements());
        assertEquals("Nearby Hotel", result.getHotels().getContent().get(0).getName());
    }

    @Test
    @DisplayName("search -> should expand radius until 50 km when smaller radii return no hotels")
    void search_WithMatchIdOnly_ShouldExpandTo15Km() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setMatchId(100L);

        Stadium stadium = buildStadium(10L, "Mexico City", 19.3030, -99.1505);
        Match match = buildMatch(100L, stadium);
        Hotel hotel = buildHotel(2L, "City Hotel", "Ciudad de Mexico", 19.2995, -99.2140);

        RoomType rt = buildRoomType(22L, BigDecimal.valueOf(150));
        hotel.setRoomTypes(List.of(rt));

        given(matchRepository.findById(100L)).willReturn(Optional.of(match));

        stubComputedFindAllSequence(
                new PageImpl<>(List.of(), PageRequest.of(0, 500), 0),
                new PageImpl<>(List.of(), PageRequest.of(0, 500), 0),
                new PageImpl<>(List.of(), PageRequest.of(0, 500), 0),
                new PageImpl<>(List.of(hotel), PageRequest.of(0, 500), 1)
        );

        mockPrimaryPhoto(2L, "hotels/2.jpg", "url2");

        HotelCatalogResponseDto mapped = dto(2L, "City Hotel", "Ciudad de Mexico", BigDecimal.valueOf(150), 6.7);
        mockMapper(hotel, "url2", mapped);

        HotelCatalogSearchResponseDto result = service.search(pageable, criteria);

        assertEquals(HotelCatalogSearchMode.MATCH_RADIUS_50KM, result.getSearchMode());
        assertTrue(result.isFallbackApplied());
        assertEquals(1, result.getHotels().getTotalElements());
        assertEquals("City Hotel", result.getHotels().getContent().get(0).getName());
    }

    @Test
    @DisplayName("search -> should expand radius until 50 km when 5 km and 15 km and 30 km return no hotels")
    void search_WithMatchIdOnly_ShouldExpandTo30Km() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setMatchId(100L);

        Stadium stadium = buildStadium(10L, "Monterrey", 25.6697, -100.2443);
        Match match = buildMatch(100L, stadium);
        Hotel hotel = buildHotel(3L, "Farther Hotel", "Apodaca", 25.7586, -100.2153);

        RoomType rt = buildRoomType(33L, BigDecimal.valueOf(150));
        hotel.setRoomTypes(List.of(rt));

        given(matchRepository.findById(100L)).willReturn(Optional.of(match));

        stubComputedFindAllSequence(
                new PageImpl<>(List.of(), PageRequest.of(0, 500), 0),
                new PageImpl<>(List.of(), PageRequest.of(0, 500), 0),
                new PageImpl<>(List.of(), PageRequest.of(0, 500), 0),
                new PageImpl<>(List.of(hotel), PageRequest.of(0, 500), 1)
        );

        mockPrimaryPhoto(3L, "hotels/3.jpg", "url3");

        HotelCatalogResponseDto mapped = dto(3L, "Farther Hotel", "Apodaca", BigDecimal.valueOf(150), 22.4);
        mockMapper(hotel, "url3", mapped);

        HotelCatalogSearchResponseDto result = service.search(pageable, criteria);

        assertEquals(HotelCatalogSearchMode.MATCH_RADIUS_50KM, result.getSearchMode());
        assertTrue(result.isFallbackApplied());
        assertEquals(1, result.getHotels().getTotalElements());
        assertEquals("Farther Hotel", result.getHotels().getContent().get(0).getName());
    }

    @Test
    @DisplayName("search -> should return empty page when no hotels found within 50 km")
    void search_WithMatchIdOnly_ShouldReturnEmptyAfter30Km() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setMatchId(100L);

        Stadium stadium = buildStadium(10L, "Remote City", 10.0, 10.0);
        Match match = buildMatch(100L, stadium);

        given(matchRepository.findById(100L)).willReturn(Optional.of(match));

        stubComputedFindAllSequence(
                new PageImpl<>(List.of(), PageRequest.of(0, 500), 0),
                new PageImpl<>(List.of(), PageRequest.of(0, 500), 0),
                new PageImpl<>(List.of(), PageRequest.of(0, 500), 0),
                new PageImpl<>(List.of(), PageRequest.of(0, 500), 0)
        );

        HotelCatalogSearchResponseDto result = service.search(pageable, criteria);

        assertTrue(result.isFallbackApplied());
        assertEquals(0, result.getHotels().getTotalElements());
        assertTrue(result.getHotels().getContent().isEmpty());
    }

    @Test
    @DisplayName("search -> should throw when more than one location reference is provided")
    void search_WithMultipleLocationReferences_ShouldThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setMatchId(1L);
        criteria.setStadiumId(2L);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.search(pageable, criteria)
        );

        assertTrue(ex.getMessage().contains("Only one location reference is allowed"));
    }
}