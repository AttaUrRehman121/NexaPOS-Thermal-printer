# NexaPOS: Bluetooth Reliability & Responsive Adaptive Layout Overhaul

Comprehensive architectural plan to fix Bluetooth thermal printer connectivity across Android 12–15+ (runtime permissions, insecure RFCOMM and channel 1 reflection fallback, discovery, and virtual test printer) and implement responsive adaptive layouts (tablet/landscape side-by-side split screen and compact phone optimization).

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions have been confirmed for this update:

- **Confirmed Decision 1 (Complete Bluetooth Connectivity Suite)**:
  - Add explicit Compose runtime permission launcher for `BLUETOOTH_CONNECT` and `BLUETOOTH_SCAN` on Android 12+ (API 31+).
  - Implement a 3-tier RFCOMM connection sequence:
    1. Standard secure RFCOMM with SPP UUID `00001101-0000-1000-8000-00805F9B34FB`
    2. Insecure RFCOMM (`createInsecureRfcommSocketToServiceRecord`)
    3. Direct reflection to channel 1 (`createRfcommSocket(1)`) for POS-58, Xprinter, Goojprt, and PT-210 printers.
  - Add active Bluetooth discovery and paired device listing with Bluetooth enable prompt.
  - Provide a built-in "Virtual Thermal Printer (Test Mode)" so cashiers/users can test real-time ESC/POS printing in both emulator and hardware environments.
- **Confirmed Decision 2 (Adaptive Responsive Design)**:
  - Implement adaptive window sizing:
    - **Expanded & Medium Screens (Tablets / Landscape >= 600dp)**: Dual-pane layout where product catalog and category filters occupy the left 60% and the active cart counter occupies the right 40% side-by-side, eliminating bottom sheet friction.
    - **Compact Screens (Phones in Portrait)**: Optimized single-column catalog with floating cart pill and smooth bottom sheet.
  - Constrain all dialogs (Payment, Receipt, Scanner, Product Editor) with `Modifier.widthIn(max = 540.dp)` to prevent edge stretching on wide displays.
  - Full edge-to-edge support respecting system navigation bars and status bar insets.

---

## 1. Technical Architecture & Changes

```
┌────────────────────────────────────────────────────────────────────────┐
│                   Responsive UI Layer (BoxWithConstraints)             │
├───────────────────────────────────┬────────────────────────────────────┤
│     Compact Mode (< 600dp)        │      Expanded Mode (>= 600dp)      │
│  - Full-width product catalog     │  - Left Pane (60%): Catalog & Grid │
│  - Floating bottom cart bar       │  - Right Pane (40%): Sticky Cart   │
│  - Modal bottom sheet cart        │  - Direct simultaneous checkout    │
└───────────────────────────────────┴────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                  Bluetooth Printer Engine (Resilient)                  │
│  ┌───────────────────────┐         ┌─────────────────────────────────┐ │
│  │ Runtime Permissions   │         │ 3-Stage RFCOMM Fallback Engine  │ │
│  │ (BLUETOOTH_CONNECT /  │────────▶│ 1. Standard SPP UUID            │ │
│  │  BLUETOOTH_SCAN)      │         │ 2. Insecure RFCOMM              │ │
│  └───────────────────────┘         │ 3. Reflection Channel 1         │ │
│                                    └─────────────────────────────────┘ │
│                                                    │                   │
│                                                    ▼                   │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Virtual ESC/POS Test Driver (Instant zero-hardware validation)   │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Key Product Decisions & Implementation Steps

1. **Bluetooth Engine Fixes (`ThermalPrinterManager.kt`)**:
   - Provide runtime permission verification before accessing Bluetooth adapter.
   - Implement `connectWithFallback()`:
     - Attempt 1: `device.createRfcommSocketToServiceRecord(SPP_UUID)`
     - Attempt 2: `device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)`
     - Attempt 3: `device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType).invoke(device, 1)`
   - Add discovery receiver for unpaired devices nearby.
   - Include virtual test device option (`Virtual 58mm Thermal Printer` and `Virtual 80mm Counter Printer`) with live print logs.
2. **In-Settings & In-App Bluetooth UI**:
   - Add "Bluetooth Permission Required" alert banner with a "Grant Bluetooth Permission" button.
   - Add Bluetooth power toggle check (`Enable Bluetooth` prompt).
   - Show printer signal type (Bluetooth, USB, Network, Virtual Test).
3. **Responsive Adaptive Layouts**:
   - Refactor `PosCheckoutScreen.kt` using `BoxWithConstraints`:
     - If `maxWidth >= 600.dp`, display horizontal row: Product Catalog on left, Cart on right.
     - If `maxWidth < 600.dp`, display vertical layout with bottom bar.
   - Standardize dialog max widths (`Modifier.widthIn(max = 540.dp)`) across `ReceiptModal`, `PaymentDialog`, `BarcodeScannerDialog`, and `ProductEditorDialog`.
4. **Verification**:
   - Run `compile_applet` and execute unit tests.
