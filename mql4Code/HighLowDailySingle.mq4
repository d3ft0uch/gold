extern double Lots=0.1;
extern double StopLoss=20;
extern double TakeProfit=5;
extern int MaxSpread=30;
extern int Magic = 12;

string  openposition, LockBar_s, spread_s;
double  TP=0;
bool opentrade, LockBar=false, Trade=false;
int spread, total, i, ticket, Handle;

datetime newBar;
double spreadAtOpen = 0;
double currentProfit = 0;
double pipVal = 0;

double lastHigh;
double lastLow;


void readindicators()
   {      
      spread=MarketInfo(Symbol(),MODE_SPREAD);
   }
   
   
void checkorders()
   {
      total=OrdersTotal();
      opentrade=false;
      for(i=0; i<total; i++)
         {
            if(OrderSelect(i,SELECT_BY_POS)==false) continue;
            if (OrderMagicNumber()==Magic && OrderSymbol() == Symbol() && (OrderType() == OP_BUY || OrderType() == OP_SELL)) {opentrade=true; ticket=OrderTicket();}
         }
      if(opentrade == false)
      {
         TP = 0;
      }
   }   
   
void display_conditions()
   {   
      Comment("Spread: ", spread, "\n", "\n","high: " 
         + iHigh(Symbol(), PERIOD_H1, 1), "\n","Ask: " + Ask+ "\n","Low: " +  iLow(Symbol(), PERIOD_H1, 1),"\n","Bid: " + Bid, "\n","pipVal: " + pipVal
      
      );
   }
   
   
bool NewBar()
{
   if(lastHigh != iHigh(Symbol(), PERIOD_H1, 1))
   {
      lastHigh = iHigh(Symbol(), PERIOD_H1, 1);
      return(true);
   }
   return (false);
}
   
void managetrades()
   {
      
      if (opentrade) {      
      
         OrderSelect(ticket, SELECT_BY_TICKET);
         double pipValBeforeOanda = getPipValue(OrderOpenPrice(),OrderType());
         pipVal = pipValBeforeOanda/10;
         
         if(currentProfit>0)Print("Profit for " + Symbol() + " " + pipVal + "past profit is " + currentProfit);
         
         if(pipVal > 5 || pipVal>currentProfit)
         {
            currentProfit = pipVal + 5;
            //current = 2.50
         }
         //2 < 1.50
         if(pipVal > (currentProfit - 6) && (pipVal > 0 && currentProfit != 0))
         {
            if (OrderType()==OP_BUY) if (OrderClose(ticket, Lots, Bid, 10, Red)==false)
                  for (i=0; i==20; i++) if (OrderClose(ticket, Lots, Bid, 10, Red)==true) continue;
  
            if (OrderType()==OP_SELL)  if (OrderClose(ticket, Lots, Ask, 10, Red)==false)
                  for (i=0; i==20; i++) if (OrderClose(ticket, Lots, Ask, 10, Red)==true) continue;

            currentProfit = 0;

         }
         
      }      
      
   }

double getPipValue(double ord,int dir)
{
   double val;
   RefreshRates();
   if(dir == 1) val=(NormalizeDouble(ord,Digits) - NormalizeDouble(Ask,Digits));
   else val=(NormalizeDouble(Bid,Digits) - NormalizeDouble(ord,Digits));
   val = val/Point;
   return(val);   
}


void closePendingOrders()
{
   total=OrdersTotal();
   for(i=0; i<total; i++)
   {
      if(OrderSelect(i,SELECT_BY_POS)==false) continue;
      if (OrderMagicNumber()==Magic && OrderSymbol() == Symbol() && (OrderType() == OP_BUYSTOP || OrderType() == OP_SELLSTOP)) 
      {
         if(OrderType() == OP_SELLSTOP) 
         { 
            if(OrderDelete(OrderTicket()) == false)
               for (i=0; i==20; i++) 
               if (OrderDelete(OrderTicket())==true) continue; 
            else
               Print("Close sell successful");
            Sleep(500);
         }
         if(OrderType() == OP_BUYSTOP) 
         {
            if(OrderDelete(OrderTicket()) == false)
             for (i=0; i==20; i++) if (OrderDelete(OrderTicket()) ) continue;
            else
               Print("Close buy successful");
               
             Sleep(500);
         }
      }
   }
}


void closeOpenOrders()
{
   total=OrdersTotal();
   for(i=0; i<total; i++)
   {
      if(OrderSelect(i,SELECT_BY_POS)==false) continue;
      if (OrderMagicNumber()==Magic && OrderSymbol() == Symbol() && (OrderType() == OP_BUY || OrderType() == OP_SELL)) 
      {
         if(OrderType() == OP_SELL) 
         { 
            if (OrderClose(ticket, Lots, Ask, 10, Red)==false)
               for (i=0; i==20; i++) 
                  if (OrderClose(ticket, Lots, Ask, 10, Red)==true) continue;
         }
         if(OrderType() == OP_BUY) 
         {
            if (OrderClose(ticket, Lots, Bid, 10, Red)==false)
               for (i=0; i==20; i++) 
                  if (OrderClose(ticket, Lots, Bid, 10, Red)==true) continue;
         }
      }
   }
}



void start()
  {
   
   if(NewBar()) {
   
      closePendingOrders();
      closeOpenOrders();
      pipVal = 0;
      
      

      total=OrdersTotal();
      bool openNewTrade = false;
      
      for(i=0; i<total; i++)
      {
         if(OrderSelect(i,SELECT_BY_POS)==false) continue;
         if (OrderMagicNumber()==Magic && OrderSymbol() == Symbol() && (OrderType() == OP_BUYSTOP || OrderType() == OP_SELLSTOP)) 
         {
            openNewTrade = true;
         }
      }




      if(openNewTrade == false)
      {
         double high = NormalizeDouble(iHigh(Symbol(), PERIOD_H1, 1), Digits);
         double stoplossHigh=NormalizeDouble(high-StopLoss*Point,Digits);   
         double takeprofitHigh=NormalizeDouble(high+TakeProfit*Point,Digits);
         OrderSend(Symbol(),OP_BUYSTOP,Lots,high,3,0,0,"My order",Magic,0,clrGreen);
         
         //Sleep(500); 
         double low = NormalizeDouble(iLow(Symbol(), PERIOD_H1, 1), Digits);
         double stoplossLow=NormalizeDouble(low+StopLoss*Point,Digits);   
         double takeprofitLow=NormalizeDouble(low-TakeProfit*Point,Digits);
         OrderSend(Symbol(),OP_SELLSTOP,Lots,low,3,0,0,"My order",Magic,0,clrBlueViolet);
      }
     
   
   }
   readindicators();
   checkorders();
   //checkconditions();
   display_conditions();
   //newtrades();
   managetrades();

   return(0);
  }

