# World Cup Hotel Booking — Project Structure

This document explains the package/module layout for the Spring Boot project.

---

## Package Tree

```text
com.worldcup.hotelbooking
│com.worldcup.hotelbooking
│
├── availability_pricing
│   ├── availability
│   │   ├── AvailabilityController.java
│   │   ├── AvailabilityService.java
│   │   └── dto/
│   │
│   ├── match
│   │   ├── Match.java
│   │   ├── MatchRepository.java
│   │   ├── MatchService.java
│   │   ├── MatchController.java
│   │   └── dto/
│   │
│   ├── pricing
│   │   ├── PricingService.java
│   │   ├── EnhancedPricingService.java
│   │   ├── PricingConfig.java
│   │   └── dto/
│   │
│   └── stadium
│       ├── Stadium.java
│       ├── StadiumRepository.java
│       ├── StadiumService.java
│       ├── StadiumController.java
│       └── dto/
│
├── booking
│   ├── booking
│   │   ├── Booking.java
│   │   ├── BookingRepository.java
│   │   ├── BookingService.java
│   │   ├── BookingController.java
│   │   ├── BookingSpecifications.java
│   │   ├── BookingMapper.java
│   │   ├── exceptions/
│   │   └── dto/
│   │
│   ├── bookingroom
│   │   ├── BookingRoom.java
│   │   ├── BookingRoomRepository.java
│   │   ├── BookingRoomService.java
│   │   ├── BookingRoomController.java
│   │   ├── BookingRoomMapper.java
│   │   ├── exceptions/
│   │   └── dto/
│   │
│   └── cancellation
│       ├── CancellationPolicyService.java
│       ├── CancellationMapper.java
│       ├── CancellationResult.java
│       └── dto/
│
├── catalog
│   ├── hotel
│   │   ├── Hotel.java
│   │   ├── HotelRepository.java
│   │   ├── HotelService.java
│   │   ├── HotelController.java
│   │   ├── HotelStatus.java
│   │   ├── exceptions/
│   │   ├── mapper/
│   │   └── dto/
│   │
│   ├── query
│   │   └── hotel
│   │       ├── HotelCatalogController.java
│   │       ├── HotelCatalogService.java
│   │       ├── HotelCatalogServiceImpl.java
│   │       ├── HotelCatalogSpecifications.java
│   │       ├── HotelCatalogCriteria.java
│   │       ├── mapper/
│   │       └── dto/
│   │
│   ├── roomtype
│   │   ├── RoomType.java
│   │   ├── RoomTypeRepository.java
│   │   ├── RoomTypeService.java
│   │   ├── RoomTypeController.java
│   │   ├── exceptions/
│   │   ├── mapper/
│   │   └── dto/
│   │
│   ├── hotelphoto
│   │   ├── HotelPhoto.java
│   │   ├── HotelPhotoRepository.java
│   │   ├── HotelPhotoService.java
│   │   └── dto/
│   │
│   ├── roomtypephoto
│   │   ├── RoomTypePhoto.java
│   │   ├── RoomTypePhotoRepository.java
│   │   ├── RoomTypePhotoService.java
│   │
│   └── storage
│       ├── PhotoUrlResolver.java
│       └── LocalPhotoUrlResolver.java
│
├── payment
│   └── payment
│       ├── Payment.java
│       ├── PaymentRepository.java
│       ├── PaymentService.java
│       ├── PaymentController.java
│       ├── PaymentMapper.java
│       ├── exceptions/
│       └── dto/
│
├── notification
│   └── notification
│       ├── Notification.java
│       ├── NotificationRepository.java
│       ├── NotificationService.java
│       └── dto/
│
├── user
│   └── user
│       ├── AppUser.java
│       ├── AppUserRepository.java
│       ├── AppUserService.java
│       ├── AppUserController.java
│       ├── AppUserMapper.java
│       ├── Role.java
│       ├── exceptions/
│       └── dto/
│
├── common
│   ├── enums/
│   ├── exception/
│   ├── mapper/
│   ├── response/
│   └── OpenApiConfig.java
│
└── WorldCupHotelBookingApplication.java
```

---

## How to Read This Structure

This layout is **feature-based at the module level** (catalog, booking, payment, …), and **feature-subpackages** inside
each module (hotel, room, match, …).

Each feature typically contains:

- **Entity (`*.java`)**: JPA model / domain object
- **Repository**: DB access (`JpaRepository`, custom queries, etc.)
- **Service**: business logic + orchestration
- **Controller**: REST endpoints
- **dto/**: request/response models for API boundaries

> Goal: keep everything related to one feature close together, making it easier to split into microservices later.

---

## Modules Overview

### 1) `catalog/`

Responsible for property content and browsing:

- Hotels, room types/rooms
- Photos for hotels and rooms
- Filtering, pagination, search (as needed)

**Sub-features**

- `hotel/`: Hotel CRUD + Hotel DTOs
- `room/`: Room/RoomType CRUD + DTOs
- `hotelphoto/`, `roomphoto/`: Photo metadata + DB relations (file storage handled elsewhere)

---

### 2) `availability_pricing/`

Responsible for availability windows and pricing logic:

- Stadiums and matches (World Cup context)
- Availability calculation per room type/room
- Pricing calculation rules

**Sub-features**

- `stadium/`: Stadium data (location, city, etc.)
- `match/`: Matches and dates (used for peak pricing / demand)
- `availability/`: endpoints + service for availability checks
- `pricing/`: endpoints + service for pricing computation

> If pricing rules are hard-coded, you may not need a `PricingRule` table.
> If they become dynamic, add a pricing domain model (rules, seasons, overrides).

---

### 3) `booking/`

Responsible for the booking lifecycle:

- Create booking
- Attach one or more booked room types / rooms
- Cancel booking (based on policy)
- Booking status transitions

**Sub-features**

- `booking/`: Booking aggregate root + lifecycle endpoints
- `bookingroom/`: Booking line items (e.g., `BookingRoom` or `BookingRoomType`)

---

### 4) `payment/`

Responsible for payment records and payment flow:

- Create payment intent/record
- Track status
- Refund handling (if applicable)

**Sub-feature**

- `payment/`: Payment domain + endpoints + DTOs

---

### 5) `notification/`

Responsible for notifications and audit log:

- Email/SMS/push send request (optional controller)
- Log notification attempts/results

**Sub-feature**

- `notification/`: Notification domain + service + DTOs

---

### 6) `appUser/`

Responsible for appUser management:

- User CRUD
- Roles/permissions integration (if/when added)
- Profile data

**Sub-feature**

- `appUser/`: User domain + endpoints + DTOs

---

## Cross-Cutting Concerns — `common/`

### `common/exception/`

- `ApiException`: custom exception type for clean error handling
- `GlobalExceptionHandler`: centralized exception-to-response mapping

### `common/enums/`

Shared enums used across features (avoid duplication):

- `BookingStatus`
- `PaymentStatus`

### `common/response/`

Unified API response wrapper:

- `ApiResponse`

### `common/mapper/`

Reusable mapping utilities:

- `EntityMapper` (base mapper interface/helper)

> Tip: keep `common/` minimal. Do **not** move business logic here.

---

## Naming & Conventions (Recommended)

- **Controllers**: `XController` (REST only; no business logic)
- **Services**: `XService` (business rules + orchestration)
- **Repositories**: `XRepository` (DB queries only)
- **DTOs**: `XRequestDto`, `XResponseDto`
- **Packages**: lowercase, singular when possible (`hotel`, `room`, `match`)

---

## Microservices Readiness Notes

This structure is already close to a microservices split:

- `catalog/**` → `catalog-service`
- `availability_pricing/**` → `availability-pricing-service`
- `booking/**` → `booking-service`
- `payment/**` → `payment-service`
- `notification/**` → `notification-service`
- `appUser/**` → `appUser-service`

When splitting:

- Prefer **separate DB per service**
- Share only **contracts** (OpenAPI schemas / event schemas), not shared domain code.

---

## Entry Point

- `WorldCupHotelBookingApplication.java`: Spring Boot application bootstrap.
