# Free VPN Gate - تطبيق VPN مجاني للأندرويد

تطبيق أندرويد متكامل ومفتوح المصدر للاتصال بخوادم VPN المجانية العامة المقدمة من متطوعين حول العالم عبر مشروع **VPN Gate** الأكاديمي (جامعة تسوكوبا - اليابان).

تم بناء التطبيق باستخدام أحدث تقنيات أندرويد الرسمية: **Kotlin** و **Jetpack Compose** و **Material Design 3**.

---

## 🚀 المميزات الرئيسية (Key Features)

1. **جلب خوادم VPN Gate الحية**:
   - الاتصال بواجهة برمجة التطبيقات الرسمية: `https://www.vpngate.net/api/iphone/`.
   - تحليل ملفات CSV واستخراج بيانات السيرفرات وتكوينات OpenVPN المشفرة بـ Base64.
2. **اختبار سرعة الاستجابة الفعلية (Real Ping/Latency)**:
   - فحص زمن استجابة مقبس الشبكة (Socket RTT) مباشرةً لكل سيرفر بالمللي ثانية، مع تمييز لوني (أخضر / أصفر / أحمر).
3. **الاتصال بنفق VpnService**:
   - استخدام واجهة `android.net.VpnService` لإنشاء النفق الشبكي وحماية المقابس وتوجيه البيانات.
   - دعم كامل لخدمات أندرويد الأمامية (Foreground Service) مع إشعار دائم يعرض مدة الاتصال وحجم البيانات المتدفقة وزر للقطع السريع.
4. **المفضلة والتخزين المحلي**:
   - حفظ السيرفرات المفضلة والذاكرة المؤقتة محلياً عبر قاعدة بيانات **Room**.
5. **تحديث تلقائي في الخلفية**:
   - استخدام **WorkManager** لجدولة تحديث الخوادم بشكل دوري كل 6 ساعات عند توفر اتصال إنترنت.
6. **فرز وبحث متقدم**:
   - فلترة حسب الدولة أو اسم الخادم أو عنوان IP.
   - ترتيب حسب: أقل بنغ (الأسرع)، أعلى سرعة، أو أفضل تقييم.
   - تصفية حسب الحد الأدنى للسرعة (Mbps).
7. **عارض وناسخ تكوين OpenVPN**:
   - استعراض ملف التكوين كاملاً (`.ovpn`) ونسخه بنقرة زر لاستخدامه في أي تطبيق خارجي إذا رغبت.
8. **دعم كامل للغتين العربية والإنجليزية**:
   - نصوص وموارد كاملة في `res/values/strings.xml` و `res/values-ar/strings.xml` مع مراعاة اتجاه الكتابة RTL/LTR.
9. **تحذير الأمان والشفافية**:
   - تنبيه صريح للمستخدم بأن الخوادم عامة وتطوعية ويجب استخدامها بمسؤولية للتصفح العادي.

---

## 🛠 البنية المعمارية والتقنيات (Architecture & Stack)

- **UI**: Jetpack Compose مع Material 3، بتصميم داكن مستوحى من واجهات الأمن السيبراني (Cyber Navy & Cyan).
- **Architecture**: MVVM + Repository Pattern.
- **Data Persistence**: Android Room Database (مع KSP).
- **Networking**: OkHttp Client مع مهلات ذكية ومعالجة الروابط البديلة.
- **Background Tasks**: Android WorkManager (CoroutineWorker).
- **Concurrency**: Kotlin Coroutines & StateFlow / SharedFlow.
- **Testing**: Robolectric + JUnit.

---

## 📂 هيكل المجلدات الرئيسي (Directory Structure)

```
app/src/main/
├── AndroidManifest.xml                  # تعريف الأذونات و VpnService و Foreground Service
├── java/com/example/
│   ├── FreeVpnApplication.kt            # نقطة انطلاق التطبيق وجدولة WorkManager
│   ├── MainActivity.kt                  # النشاط الرئيسي ونظام التبويبات وطلب أذونات VPN
│   ├── model/
│   │   ├── VpnServer.kt                 # نموذج الخادم والسرعة والأعلام التعبيرية
│   │   ├── VpnStatus.kt                 # آلة حالات الاتصال وإحصائيات الجلسة
│   │   └── FilterSortOptions.kt         # خيارات الفلترة والترتيب
│   ├── data/
│   │   ├── local/
│   │   │   ├── AppDatabase.kt           # قاعدة بيانات Room
│   │   │   ├── VpnServerDao.kt          # استعلامات التخزين والمفضلة
│   │   │   └── VpnServerEntity.kt       # جدول الخوادم
│   │   ├── remote/
│   │   │   ├── VpnGateApiService.kt     # جلب CSV من خوادم VPN Gate
│   │   │   ├── VpnGateCsvParser.kt      # محلل ملفات CSV وفك Base64
│   │   │   └── PingTester.kt            # فاحص البنغ الحقيقي عبر TCP Socket
│   │   └── repository/
│   │       └── VpnRepository.kt         # دمج المصادر والتحكم بالخوادم
│   ├── vpn/
│   │   ├── FreeVpnService.kt            # خدمة VpnService وإنشاء النفق والإشعار
│   │   ├── VpnController.kt             # إدارة حالة الاتصال والتواصل مع واجهة المستخدم
│   │   └── OpenVpnConfigParser.kt       # استخراج المنافذ والبروتوكول والتشفير
│   ├── worker/
│   │   └── RefreshServersWorker.kt      # مهمة التحديث الدوري بالخلفية
│   └── ui/
│       ├── components/
│       │   ├── ConnectionControlCard.kt # بطاقة التحكم الرئيسية وزر الطاقة النبضي
│       │   ├── ServerCard.kt            # بطاقة الخادم والأزرار وشارات القياس
│       │   ├── ConfigPreviewDialog.kt   # حوار استعراض ونسخ ملف .ovpn
│       │   └── SecurityDisclaimerDialog.kt # تنبيه شبكات المتطوعين العامة
│       ├── screens/
│       │   ├── HomeScreen.kt            # الشاشة الرئيسية
│       │   ├── FavoritesScreen.kt       # شاشة المفضلة
│       │   └── SettingsScreen.kt        # شاشة الإعدادات
│       └── theme/                       # السمات والألوان المخصصة
└── res/
    ├── values/strings.xml               # النصوص الإنجليزية
    └── values-ar/strings.xml            # النصوص العربية
```

---

## 🔨 خطوات بناء التطبيق عبر Android Studio

1. **متطلبات النظام**:
   - تثبيت **Android Studio Hedgehog (2023.1.1)** أو أحدث (مثل Koala أو Ladybug).
   - JDK 17 أو JDK 21.
   - Android SDK مع منصة Build Tools 34 فما فوق.

2. **فتح المشروع**:
   - افتح Android Studio واختر **Open**.
   - حدد المجلد الرئيسي للمشروع وانتظر حتى يكتمل تزامن **Gradle Sync**.

3. **تشغيل التطبيق**:
   - اختر جهازاً حقيقياً أو محاكياً بنظام أندرويد 7.0 (API 24) فما فوق.
   - اضغط على زر التشغيل الأخضر **Run 'app'** (`Shift + F10`).

4. **توليد ملف APK قابل للتثبيت**:
   - لبناء نسخة Debug APK:
     ```bash
     gradle assembleDebug
     ```
     ستجد الملف في المسار:
     `app/build/outputs/apk/debug/app-debug.apk`
   - لبناء نسخة Release APK:
     من القائمة العلوية في أندرويد ستوديو، اختر **Build > Generate Signed Bundle / APK**، ثم اختر **APK** وقم باختيار مفتاح التوقيع ومتابعة الخطوات.

---

## ⚖️ إخلاء المسؤولية القانونية والأخلاقية (Disclaimer)

- يعتمد هذا التطبيق حصرياً على خوادم تجريبية وأكاديمية عامة موفرة من قبل متطوعين عبر مشروع **VPN Gate** التابع لجامعة تسوكوبا باليابان.
- التطبيق **لا يجمع أي بيانات شخصية أو سجلات تصفح للمستخدمين** إطلاقاً.
- التطبيق خالي من الإعلانات ومن أدوات التعقب الخارجية.
- يرجى استخدام التطبيق وفقاً للقوانين واللوائح المعمول بها في منطقتك الجغرافية.
