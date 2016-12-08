#property copyright "Copyright © 2007, MetaQuotes Software Corp."
#property link      "http://www.metaquotes.net"

#property indicator_chart_window
#property indicator_buffers 1
#property indicator_color1 Blue

extern int Lookback = 10;
double ssl[];

int init() {
   SetIndexBuffer(0, ssl);   
   return (0);
}

int deinit() {
   return (0);
}

int start() {
   int Hlv;
   int Hld;
   for (int i = Bars - Lookback; i >= 0; i--) {
      if (Close[i] > iMA(NULL, 0, Lookback, 0, MODE_SMA, PRICE_HIGH, i + 1)) 
         Hld = 1;
      else {
         if (Close[i] < iMA(NULL, 0, Lookback, 0, MODE_SMA, PRICE_LOW, i + 1)) 
            Hld = -1;
         else 
            Hld = 0;
      }
      
      if (Hld != 0) 
         Hlv = Hld;
      
      if (Hlv == -1) 
         ssl[i] = iMA(NULL, 0, Lookback, 0, MODE_SMA, PRICE_HIGH, i);
      else 
         ssl[i] = iMA(NULL, 0, Lookback, 0, MODE_SMA, PRICE_LOW, i);
         
   }
   //Comment("HiLo= ", ssl[i + 1]);
   return (0);
}
