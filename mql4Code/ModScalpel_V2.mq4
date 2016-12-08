extern double Lots=0.1;
extern double StopLoss=150;
extern double TrailingStop=150;
extern bool UseTrailingStop=true;
extern bool SetTakeProfitOnPivots=false;
extern double TakeProfit=300;
extern bool UseTakeProfit=true;
extern int MaxSpread=10;
extern bool TradeOnNewBar=true;
extern bool ShowPopUpWindows=false;
extern bool UsePivotsForEntry=false;
extern bool UseEMAForEntry=true;
extern int Magic = 12;

#define VERSION "1.26"

string MACD, Lag, Stochs, Final, MACDcross_s, Pivs, EMAs, openposition, LockBar_s, spread_s, TradingOpenNewBar="", CommentOrder, nowStr, Ganns;
double MACDSignal, MACDSignalprev, MACDCurrent, MACDCurrentprev, MACDHist, Lag40, Lag40prev, Lag60, Lag60prev, Lag60prev2, Lag80, Lag80prev, Lag80prev2, gannPrev, gannPrev2, Stoch, Piv, TP=0, 
EMA50, EMA200, gann, RSI;
bool MACDbuy, MACDsell, Lagbuy, Lagsell, GannBuy, GannSell, Stochbuy, Stochsell, PivBuy, PivSell, EMABuy, EMASell, Buy, Sell, MACDclose, opentrade, LockBar=false, Trade=false, LagTimeToCloseBuy=false, LagTimeToCloseSell=false;
int Min=100, spread, total, i, ticket, Handle, Qnt_Symb;
string File_Name="candles.csv";
bool lag60ShouldGoUp = false;
bool lag60ShouldGoDown = false;



void readindicators()
   {
      
      MACDCurrent=iCustom(NULL,0,"MACDTraditional",12,20,9,0,0,0);
      MACDCurrentprev=iCustom(NULL,0,"MACDTraditional",12,20,9,0,0,1);
      MACDSignal=iCustom(NULL,0,"MACDTraditional",12,20,9,0,1,0);
      MACDSignalprev=iCustom(NULL,0,"MACDTraditional",12,20,9,0,1,1);      
      MACDHist=iCustom(NULL,0,"MACDTraditional",12,20,9,0,2,0);
      
      Lag40=iCustom(NULL,0,"Laguerre-ACS1",0.4,9500,2,0,0,0);
      Lag40prev=iCustom(NULL,0,"Laguerre-ACS1",0.4,9500,2,0,0,1);
      Lag60=iCustom(NULL,0,"Laguerre-ACS1",0.60,9500,2,0,0,0);
      Lag60prev=iCustom(NULL,0,"Laguerre-ACS1",0.60,9500,2,0,0,1);
      Lag60prev2=iCustom(NULL,0,"Laguerre-ACS1",0.60,9500,2,0,0,2);
      Lag80=iCustom(NULL,0,"Laguerre-ACS1",0.80,9500,2,0,0,0);
      Lag80prev=iCustom(NULL,0,"Laguerre-ACS1",0.80,9500,2,0,0,1);
      Lag80prev2=iCustom(NULL,0,"Laguerre-ACS1",0.80,9500,2,0,0,2);
      
      Stoch=iStochastic(NULL,0,14,3,3,MODE_SMA,0,MODE_MAIN,0)-50;
      spread=MarketInfo(Symbol(),MODE_SPREAD);
      Piv=(iHigh(NULL,1440,1)+iLow(NULL,1440,1)+iClose(NULL,1440,1))/3;
      
      EMA50=iMA(NULL,PERIOD_H4,50,0,MODE_EMA,PRICE_CLOSE,0);
      EMA200=iMA(NULL,0,200,0,MODE_EMA,PRICE_CLOSE,0);            
      RSI = iRSI(NULL, PERIOD_H4,14,PRICE_CLOSE,0);
      gann=iCustom(NULL,0,"Gann_HiLo_Activator_v2_mod_2",10,0,1);         
      gannPrev=iCustom(NULL,0,"Gann_HiLo_Activator_v2_mod_2",10,0,2);
      gannPrev2=iCustom(NULL,0,"Gann_HiLo_Activator_v2_mod_2",10,0,3);
   }
   
   

void SetArrow(int cd, color cl, string nm="", datetime t1=0, double p1=0, int sz=0, int margin=0) {

   nm=nm+DoubleToStr(Time[0], 0);
   if (t1<=0) t1=Time[0];
   if (p1<=0) p1=Bid;
   
   if (ObjectFind(nm)<0) ObjectCreate(0, nm, OBJ_TEXT, 0, t1, p1 + (margin * Point));
   ObjectSetText(nm, CharToStr(cd), 20, "Wingdings", cl);
} 
   
void write_indicators_to_file() {
/*   
   nowStr = TimeToStr(TimeCurrent(),TIME_DATE|TIME_SECONDS);      
   
   Handle=FileOpen(Symbol() + "_" + Period() + "_" + TimeCurrent() + "_" + File_Name, FILE_CSV|FILE_WRITE, ";");//�������� ����� |FILE_READ
   if(Handle==-1) {
      Alert("������ ��� �������� �����. ",// ��������� �� ������ 
              "��������, ���� ����� ������ �����������");      
      return;                          // ����� �� start()      
   }
//--------------------------------------------------------------- 6 --
   
   //FileSeek(Handle, 0, SEEK_END);
     
   for (int iii=3000; iii>=0; iii--){
      Qnt_Symb=FileWrite(Handle, nowStr, Open[iii], "", "", High[iii], "", "", Low[iii], "", "", Close[iii], "", "", iMA(NULL,0,5,0,MODE_EMA,PRICE_CLOSE,iii), 
      iMA(NULL,0,60,0,MODE_EMA,PRICE_CLOSE,iii), iMA(NULL,0,200,0,MODE_EMA,PRICE_CLOSE,iii),
      iCustom(NULL,0,"Laguerre-ACS1",0.6,1000,2,0,0,iii), iCustom(NULL,0,"Laguerre-ACS1",0.6,1000,2,0,0,iii+1), iCustom(NULL,0,"Laguerre-ACS1",0.8,1000,2,0,0,iii), iCustom(NULL,0,"Laguerre-ACS1",0.8,1000,2,0,0,iii+1));//������ � ����
         
      if(Qnt_Symb < 0) {                 // ���� �� ����������   
         Alert("������ ������ � ���� ", GetLastError());// ���������
         PlaySound("Bzrrr.wav");       // �������� �������������
         FileClose( Handle );          // ��������� ����
         return;                       // ����� �� start()      
      }
   }
   

//--------------------------------------------------------------- 7 --
   FileClose( Handle );                // ��������� ����   
   */
   return;                             // ����� �� start()
}
   

void checkorders()
   {
      total=OrdersTotal();
      opentrade=false;
      for(i=0; i<total; i++)
         {
            if(OrderSelect(i,SELECT_BY_POS)==false) continue;
            if (OrderMagicNumber()==Magic) {opentrade=true; ticket=OrderTicket();}
         }
   }   
   
void checkconditions()
   {
 
 //   Lag
                          
      
      
      
      if (Lag60<=0.00) 
      {
         lag60ShouldGoUp = true;
         lag60ShouldGoDown = false;
         //SetArrow(228, clrRed, "lag60ShouldGoUp", 0, Ask, 0, 8);   //Lag40 up
      }
      
      if(Lag60>=0.30)
      {
         lag60ShouldGoUp = false;
      }
      
      if (Lag60>=1.00) 
      {
         lag60ShouldGoDown = true;
         lag60ShouldGoUp = false;
         //SetArrow(230, clrRed, "lag60ShouldGoDown", 0, Bid, 0, -8);   //Lag40 down
      }
      
      if (Lag60<=0.70) 
      {
         lag60ShouldGoDown = false;
         //SetArrow(230, clrRed, "lag60ShouldGoDown", 0, Bid, 0, -8);   //Lag40 down
      }
      
      if (lag60ShouldGoUp && (Lag60>Lag80)) 
      {
         Lagbuy=true; Lagsell=false; Lag="Lag buy";
        // SetArrow(225, clrYellow, "LagSignalBuy", 0, Ask, 0, 13);   //Laguerre signal buy!
      } else if (lag60ShouldGoDown && (Lag60<Lag80)) {
         //SetArrow(226, clrYellow, "LagSignalSell", 0, Bid, 0, -13);   //Laguerre signal sell!
         Lagbuy=false; Lagsell=true; Lag="Lag sell";
      } else {      
         Lagbuy=false; Lagsell=false; Lag="Lag no signal " + Lag40 + " : " + Lag40prev;
      }
      
      //---if ((gann > Close[0]) || (Lag60 == 1.00)) {
      
      
      //   Gann
      if ((gannPrev >= Close[1] || gannPrev >= Open[1]) && gann < Close[0]) {
         SetArrow(196, clrRed, "GannSignalBuy", 0, Bid, 0, -14);   //���� ������ ������ ���� ��� �����   
         GannBuy=true; GannSell=false; Ganns="Gann buy";
      } else if ((gannPrev <= Close[1] || gannPrev <= Open[1]) && gann > Close[0]) {
         SetArrow(200, clrRed, "GannSignalSell", 0, Ask, 0, 14);   //���� ������ ����� ����� ��� ������
         GannBuy=false; GannSell=true; Ganns="Gann sell";
      } else {         
         GannBuy=false; GannSell=false; Ganns="Gann no signal " + gann + " : " + gannPrev  + " : " + gannPrev2;
      }
      
      
         //    Positive case                        negative case
      if (Lag60<Lag80) {
         LagTimeToCloseBuy=true;
      } else {
         LagTimeToCloseBuy=false;
      }
      
      //if ((gann < Close[0]) || (Lag60 == 0.00)) {
      //    Positive case                        negative case
      if (Lag60>Lag80) {
         LagTimeToCloseSell=true;
      } else {
         LagTimeToCloseSell=false;
      }

      

      
//    Final signal
      if (GannBuy && Lagbuy && (Close[0] > EMA50) && (RSI >=50.0) ) {
         SetArrow(241, clrLime, "SignalBuy", 0, Ask, 0, 16);   
         Buy=true; Sell=false; Final="BUY BUY BUY\n";
      } else if ( GannSell && Lagsell && (Close[0] < EMA50) && (RSI <=50.0) ) {
         SetArrow(242, clrLime, "SignalSell", 0, Bid, 0, -16);  
         Buy=false; Sell=true; Final="SELL SELL SELL\n";
      } else {         
         Buy=false; Sell=false; Final="NO SIGNAL\n";
      }
      
      
      
      //if (spread>MaxSpread) spread_s="Spread to high to trade\n"; else spread_s="";
      
//   OpenTrade
      MACDclose=false;
      MACDcross_s="";
      openposition="";
      Trade=true;

/*      if (iVolume(NULL,0,0)<11) {LockBar=false; LockBar_s=""; Trade=true; if (TradeOnNewBar) TradingOpenNewBar="New Bar OPENED, Trading Active\n";} else {TradingOpenNewBar=""; if (TradeOnNewBar) Trade=false; else Trade=true;}  //Unlock Bar

//      if (opentrade)
//        {
         if ((MACDCurrent>=MACDSignal && MACDCurrentprev<=MACDSignalprev) || (MACDCurrent<=MACDSignal && MACDCurrentprev>=MACDSignalprev)) 
            {if (ShowPopUpWindows) MACDcross_s="CLOSE TRADE  CLOSE TRADE  CLOSE TRADE \n"; MACDclose=true;}
         if (opentrade)
         openposition="OPENED POSITION\n";
//         }
*/
}
 
   
void display_conditions()
   {
      //Comment("Spread: ", spread, "   Version ", VERSION,"\n", MACD, "\n", Lag, "\n", Stochs, "\n", Pivs, "\n", EMAs, "\nGann: ", gann, "\n", TradingOpenNewBar, MACDcross_s, Final, spread_s, openposition, LockBar_s);
      
      Comment("Spread: ", spread, "\n",  Lag, "\n", Ganns, "\n", Final);
      
      /*if (MACDclose && opentrade && ShowPopUpWindows) Alert("CLOSE TRADE ", Symbol(), ", ", Period(), " min");
      if (opentrade==false && spread<=MaxSpread && LockBar==false && Trade==true && Min!=Minute() && ShowPopUpWindows)
         {
            if (Buy) {Alert("BUY ", Symbol(), ", ", Period(), " min"); Min=Minute(); PlaySound("alert.wav");}
            if (Sell) {Alert("SELL ", Symbol(), ", ", Period(), " min"); Min=Minute(); PlaySound("alert.wav");}
         }
         
       if (opentrade==false && spread<=MaxSpread && LockBar==false && Trade==true && Min!=Minute() && ShowPopUpWindows==false)
         {
            if (Buy) {PlaySound("alert.wav"); Min=Minute();}
            if (Sell) {PlaySound("alert.wav"); Min=Minute();}
         }
         */
   }
   
void tradecomment()
   {
      int Per=Period();

      switch(Per)
         {
            case 1: CommentOrder="TScalpelMod 1M"; break;
            case 5: CommentOrder="TScalpelMod 5M"; break;
            case 15: CommentOrder="TScalpelMod 15M"; break;
            case 30: CommentOrder="TScalpelMod 30M"; break;
            case 60: CommentOrder="TScalpelMod H1"; break;
            case 240: CommentOrder="TScalpelMod H4"; break;
            case 1440: CommentOrder="TScalpelMod D1"; break;
            case 10080: CommentOrder="TScalpelMod W1"; break;
            case 43200: CommentOrder="TScalpelMod MN1"; break;
            default: CommentOrder="TScalpelMod TF Unknown"; break;
         }
         
      CommentOrder=CommentOrder+"  Ver."+VERSION;
   }
   
void CalculatePivots()
   {
      double R1, R2, R3, S1, S2, S3;
      R1=2*Piv-iLow(NULL,1440,1);
      S1=2*Piv-iHigh(NULL,1440,1);
      R2=Piv-S1+R1;
      S2=Piv-R1+S1;
      R3=iHigh(NULL,1440,1)+2*(Piv-iLow(NULL,1440,1));
      S3=iLow(NULL,1440,1)-2*(iHigh(NULL,1440,1)-Piv);
      TP=0;

        // Calculate TP
      if ((Buy) && (UseTakeProfit==false))
      {
         if (Ask>R3) TP=0;
         if (Ask>R2 && Ask<R3 && (R3-Ask)<(2*StopLoss*Point)) TP=0;
         if (Ask>R2 && Ask<R3 && (R3-Ask)>(2*StopLoss*Point)) TP=R3;
         if (Ask>R1 && Ask<R2 && (R2-Ask)<(2*StopLoss*Point)) TP=R3;
         if (Ask>R1 && Ask<R2 && (R2-Ask)>(2*StopLoss*Point)) TP=R2;
         if (Ask<R1 && (R1-Ask)<(2*StopLoss*Point)) TP=R2;
         if (Ask<R1 && (R2-Ask)>(2*StopLoss*Point)) TP=R1;
      }
      
      if ((Sell) && (UseTakeProfit==false))
      {
         if (Bid<S3) TP=0;
         if (Bid<S2 && Bid>S3 && (Bid-S3)<(2*StopLoss*Point)) TP=0;
         if (Bid<S2 && Bid>S3 && (Bid-S3)>(2*StopLoss*Point)) TP=S3;
         if (Bid<S1 && Bid>S2 && (Bid-S2)<(2*StopLoss*Point)) TP=S3;
         if (Bid<S1 && Bid>S2 && (Bid-S2)>(2*StopLoss*Point)) TP=S2;
         if (Bid>S1 && (Bid-S1)<(2*StopLoss*Point)) TP=S2;
         if (Bid>S1 && (Bid-S1)>(2*StopLoss*Point)) TP=S1;
      }
   }

void CalculateTakeProfit()
   {
      if (Buy) TP=Ask+TakeProfit*Point;
      if (Sell) TP=Bid-TakeProfit*Point;   
   }

   
void newtrades()
   {
      if (opentrade==false && Trade==true)
         {
            tradecomment();
            if (SetTakeProfitOnPivots) CalculatePivots();
            if (UseTakeProfit) CalculateTakeProfit();
            if (Buy) if (OrderSend(Symbol(), OP_BUY, Lots, Ask, 10, Bid-StopLoss*Point, Bid+TakeProfit*Point, CommentOrder, Magic, 0, Green)>=0) {LockBar=true; LockBar_s="Bar Locked\n";}
            if (Sell) if (OrderSend(Symbol(), OP_SELL, Lots, Bid, 10, Ask+StopLoss*Point, Ask-TakeProfit*Point, CommentOrder, Magic, 0, Green)>=0) {LockBar=true; LockBar_s="Bar Locked\n";}
         } 
   } 
   
void managetrades()
   {
      
      if (opentrade) {      
      
         if (UseTrailingStop) {
            OrderSelect(ticket, SELECT_BY_TICKET);
            if (OrderType()==OP_BUY) {
               //if ((Bid-(TrailingStop+10)*Point > OrderStopLoss()) && (Bid-TrailingStop*Point <= (OrderOpenPrice()+TrailingStop*Point)))
               //   OrderModify(OrderTicket(),OrderOpenPrice(),Bid-Point*TrailingStop,OrderTakeProfit(),0);
               if ((Bid-(TrailingStop)*Point >= OrderOpenPrice()))
                  OrderModify(OrderTicket(),OrderOpenPrice(),OrderOpenPrice(),OrderTakeProfit(),0);
            }
               
                  
            if (OrderType()==OP_SELL) {
               //if ((Ask+(TrailingStop+10)*Point < OrderStopLoss()) && (Ask+TrailingStop*Point >= (OrderOpenPrice()-TrailingStop*Point)))
               //   OrderModify(OrderTicket(),OrderOpenPrice(),Ask+Point*TrailingStop,OrderTakeProfit(),0);
               if ((Ask+(TrailingStop)*Point < OrderOpenPrice()))
                  OrderModify(OrderTicket(),OrderOpenPrice(),OrderOpenPrice(),OrderTakeProfit(),0);
            }
         }
                  
         if (OrderType()==OP_BUY && LagTimeToCloseBuy) if (OrderClose(ticket, Lots, Bid, 10, Red)==false)
                  for (i=0; i==20; i++) if (OrderClose(ticket, Lots, Bid, 10, Red)==true) continue;
  
         if (OrderType()==OP_SELL && LagTimeToCloseSell) if (OrderClose(ticket, Lots, Ask, 10, Red)==false)
                  for (i=0; i==20; i++) if (OrderClose(ticket, Lots, Ask, 10, Red)==true) continue;
         
      }      
      
   }



void start()
  {
   
   readindicators();
   write_indicators_to_file();
   checkorders();
   checkconditions();
   display_conditions();
   newtrades();
   managetrades();

   return(0);
  }

