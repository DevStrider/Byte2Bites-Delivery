# Byte2Bites Delivery

**Byte2Bites Delivery** is the courier-facing Android app in the Byte2Bites food-delivery ecosystem. While buyers order food and sellers prepare it in the companion apps, this app is built for the people in between — the delivery drivers. It gives couriers a single place to find orders that need delivering, claim one, carry it from the seller to the buyer's door, and get rewarded for each completed trip.

## What the app does

A courier opens the app and signs in with their Byte2Bites account. From there they see a live feed of orders placed by buyers across the platform. The flow centers on a single delivery at a time:

1. **Browse** — The home screen shows a continuously updated list of orders, each as a card with the restaurant, the total, and its current status.
2. **Accept** — The courier claims an order, which assigns it to them and takes it off the available pool for others.
3. **Pick up & deliver** — They start the delivery once the food is collected, then mark it delivered when it reaches the buyer. Each order advances through `PENDING / READY → ACCEPTED → PICKED_UP → DELIVERED`.
4. **Get rewarded** — Completing a delivery awards delivery points based on the order's value, which accumulate on the courier's profile.

Tapping any order opens a full breakdown — buyer and seller names, the order total, and the drop-off address — so the driver has everything needed to complete the run.

## Delivery points

Couriers earn points scaled to the size of the order they deliver:

| Order total | Points earned |
|-------------|---------------|
| ≤ $100 | 10 |
| ≤ $150 | 15 |
| ≤ $200 | 20 |
| > $200 | 20, plus 5 more for every $50 above $200 |

These points are tracked per courier and shown on a read-only profile screen alongside their name, email, and photo.

## How it's built

The app is a native Android application written in **Kotlin**, using traditional Android Views with View Binding and Material components for the interface. Everything that needs to be shared across buyers, sellers, and couriers lives in **Firebase** — authentication is handled by Firebase Auth, and orders, users, and delivery points are stored in the Firebase Realtime Database, which is what makes the deliveries feed update live as orders come and go.

| Area | Technology |
|------|-----------|
| Language | Kotlin |
| Min / Target SDK | 24 / 34 |
| UI | Android Views + View Binding, Material Components |
| Authentication | Firebase Auth |
| Data | Firebase Realtime Database |
| Image loading | Glide |
| Animations | Lottie |
| File uploads | AWS S3 Android SDK |
| Build system | Gradle (Kotlin DSL) |

## Structure

```
app/src/main/java/com/byte2bites/delivery/
├── WelcomeActivity.kt        # Launcher; routes signed-in couriers into the app
├── LoginActivity.kt          # Firebase email/password login
├── DeliveriesActivity.kt     # Main feed of available/assigned orders
├── DeliveriesAdapter.kt      # RecyclerView adapter for the order cards
├── OrderDetailsActivity.kt   # Full order breakdown
├── ProfileActivity.kt        # Read-only courier profile + points
├── OrderDeliveryService.kt   # Firebase logic: assign, start, deliver, award points
├── PointsCalculator.kt       # Delivery-points calculation
├── Order.kt                  # Order model
└── Delivery.kt               # Delivery model
```

## Where it fits

Byte2Bites Delivery is one piece of a larger platform. Buyers and sellers register and place orders through the companion Buyer app, and account actions like password resets are handled there. This app shares the same Firebase backend, so the orders couriers see here are the very same orders buyers placed elsewhere — it's purely the delivery side of the experience.
