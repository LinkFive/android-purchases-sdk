# LinkFive Purchases Android SDK

Android SDK available:

https://pub.dev/packages/linkfive_purchases

Add the SDK to your flutter app:
```gradle
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

```gradle
	dependencies {
	        implementation 'com.github.LinkFive:android-purchases-sdk:VERSION'
	}
```

## Getting Started

Initialize the SDK
```kotlin
LinkFivePurchases.init(apiKey = "LinkFive Api Key");
```

fetch all available subscriptions:
```kotlin
LinkFivePurchases.fetchSubscriptions(context = this);
```

### Available Subscription Data

LinkFive uses LiveData to pass data to your application. You can either just use the stream or use a StreamBuilder

```kotlin
LinkFivePurchases.linkFiveSubscriptionLiveData().observe(this, subscriptionDataObserver)

val subscriptionDataObserver = Observer<LinkFiveSubscriptionData> { data: LinkFiveSubscriptionData ->
    // Update the UI.
    buildSubscription(data)
}
```

### Purchase a Subscription
Just call purchase including the sku
```kotlin
LinkFivePurchases.purchase(skuDetail.skuDetails.sku, this)
```

### Get Active Subscription Data
You will receive the data through the active subscription LiveData.
```kotlin
LinkFivePurchases.linkFiveActivePurchasesLiveData().observe(this, activeSubscriptionDataObserver)

val activeSubscriptionDataObserver = Observer<LinkFiveVerifiedPurchases> { data: LinkFiveVerifiedPurchases ->
    // Update the UI.
    handleActiveSubscription(data)
}
```

### Restore Purchases
There is no need to restore a purchase. The sdk will always load the active subscriptions after initialization.
