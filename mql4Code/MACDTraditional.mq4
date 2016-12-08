#property indicator_separate_window
#property indicator_buffers 3
#property indicator_color1 Green
#property indicator_color2 Red
#property indicator_color3 Blue

extern int FastPeriod = 12;
extern int SlowPeriod = 26;
extern int SignalPeriod = 9;
double g_ibuf_88[];
double g_ibuf_92[];
double g_ibuf_96[];
bool gi_100 = TRUE;
bool gi_104 = TRUE;

int init() {
   SetIndexStyle(0, DRAW_LINE);
   SetIndexBuffer(0, g_ibuf_88);
   SetIndexStyle(1, DRAW_LINE);
   SetIndexBuffer(1, g_ibuf_92);
   SetIndexStyle(2, DRAW_HISTOGRAM);
   SetIndexBuffer(2, g_ibuf_96);
   IndicatorDigits(MarketInfo(Symbol(), MODE_DIGITS) + 1.0);
   IndicatorShortName("MACD(" + FastPeriod + "," + SlowPeriod + "," + SignalPeriod + ")");
   SetIndexLabel(0, "MACD");
   SetIndexLabel(1, "Signal");
   SetIndexLabel(2, "Histogram");
   return (0);
}

int deinit() {
   return (0);
}

int start() {
   if (gi_100 != TRUE) return (-1);
   if (gi_104 == TRUE) {
      if (AccountNumber() == 0) return (0);
      gi_104 = FALSE;
      ArrayInitialize(g_ibuf_88, 0.0);
      ArrayInitialize(g_ibuf_92, 0.0);
      ArrayInitialize(g_ibuf_96, 0.0);
   }
   int li_0 = Bars - IndicatorCounted() + 1;
   for (int l_shift_4 = 0; l_shift_4 < li_0; l_shift_4++) {
      g_ibuf_88[l_shift_4] = iMACD(NULL, 0, FastPeriod, SlowPeriod, SignalPeriod, PRICE_CLOSE, MODE_MAIN, l_shift_4);
      g_ibuf_92[l_shift_4] = iMACD(NULL, 0, FastPeriod, SlowPeriod, SignalPeriod, PRICE_CLOSE, MODE_SIGNAL, l_shift_4);
      g_ibuf_96[l_shift_4] = g_ibuf_88[l_shift_4] - g_ibuf_92[l_shift_4];
   }
   return (0);
}
