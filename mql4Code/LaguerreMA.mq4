//+------------------------------------------------------------------
//|
//+------------------------------------------------------------------
#property copyright "mladen"
#property link      "www.forex-tsd.com"

#property indicator_chart_window
#property indicator_buffers 1
#property indicator_color1  PaleVioletRed
#property indicator_width1  2

//
//
//
//
//

extern double Gamma  = 0.85;
extern int    Price = 0;

//
//
//
//
//

double lag[];

//+------------------------------------------------------------------
//|                                                                  
//+------------------------------------------------------------------
//
//
//
//
//

int init()
{
   SetIndexBuffer(0,lag);
   return(0);
}

int deinit() { return(0); }

//+------------------------------------------------------------------
//|                                                                  
//+------------------------------------------------------------------

int start()
{
   int counted_bars=IndicatorCounted();
      if(counted_bars<0) return(-1);
      if(counted_bars>0) counted_bars--;
         int limit = MathMin(Bars - counted_bars,Bars-1);

   //
   //
   //
   //
   //
   
   for(int i = limit; i >= 0 ; i--) lag[i] = LaGuerre(iMA(NULL,0,1,0,MODE_SMA,Price,i),Gamma,i,0);
   return(0);
}


//+------------------------------------------------------------------
//|                                                                  
//+------------------------------------------------------------------
//
//
//
//
//

double workLag[][4];
double LaGuerre(double price, double gamma, int i, int instanceNo=0)
{
   if (ArrayRange(workLag,0)!=Bars) ArrayResize(workLag,Bars); i=Bars-i-1; instanceNo*=4;

   //
   //
   //
   //
   //
      
   workLag[i][instanceNo+0] = (1.0 - gamma)*price                                          + gamma*workLag[i-1][instanceNo+0];
	workLag[i][instanceNo+1] = -gamma*workLag[i][instanceNo+0] + workLag[i-1][instanceNo+0] + gamma*workLag[i-1][instanceNo+1];
	workLag[i][instanceNo+2] = -gamma*workLag[i][instanceNo+1] + workLag[i-1][instanceNo+1] + gamma*workLag[i-1][instanceNo+2];
	workLag[i][instanceNo+3] = -gamma*workLag[i][instanceNo+2] + workLag[i-1][instanceNo+2] + gamma*workLag[i-1][instanceNo+3];

   //
   //
   //
   //
   //
   
   return((workLag[i][instanceNo+0]+2.0*workLag[i][instanceNo+1]+2.0*workLag[i][instanceNo+2]+workLag[i][instanceNo+3])/6.0);
}