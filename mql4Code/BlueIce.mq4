extern double StartLot = 0.01;
extern string Note = "IF OverideXX=0 then the SL and Target will be calculated automatically";
extern int OverideSL = 0;
extern int OverideTarget = 0;
extern string Note_Coef1 = "The greater this value is, the greater SL will incur";
extern double Coef1 = 1.5;
extern string Note_Coef2 = "The greater this value is, the lesser the Target will be";
extern double Coef2 = 1.0;
extern bool DoTrailing = TRUE;
extern string Note_Climb = "If climb sets to true then the EA will normalize the profit";
extern string Note_Climb2 = "If climb sets to false then the EA will normalize the account balance";
extern bool Climb = TRUE;
extern int MaxSimultaneousOrders = 5;
extern int Magic = 12011508;

void Trailing(int MagicNum, int stopLossInternal) {
   if (stopLossInternal > 0) {
      OrderSelect(0, SELECT_BY_POS, MODE_TRADES);
      if ((OrderType() == OP_BUY || OrderType() == OP_BUYSTOP) && OrderSymbol() == Symbol() && OrderMagicNumber() == MagicNum) {
         if (stopLossInternal > 0) {
            if (Bid - OrderOpenPrice() > Point * stopLossInternal) {
               if (OrderStopLoss() < Bid - Point * stopLossInternal) {
                  OrderModify(OrderTicket(), OrderOpenPrice(), Bid - Point * stopLossInternal, OrderTakeProfit(), 0, Blue);
                  return;
               }
            }
         }
      }
      if ((OrderType() == OP_SELL || OrderType() == OP_SELLSTOP) && OrderSymbol() == Symbol() && OrderMagicNumber() == MagicNum) {
         if (stopLossInternal > 0) {
            if (OrderOpenPrice() - Ask > Point * stopLossInternal)
               if (OrderStopLoss() == 0.0 || OrderStopLoss() > Ask + Point * stopLossInternal) OrderModify(OrderTicket(), OrderOpenPrice(), Ask + Point * stopLossInternal, OrderTakeProfit(), 0, Red);
         }
      }
   }
}

int init() {
   return (0);
}

int deinit() {
   return (0);
}

int start() {
   double atrHourValue;
   double adxPlusDIArray[4];
   double adxMinusDIArray[4];
   double adxMainArray[4];
   double tickValue;
   bool setNewOrderLotFlag;
   
   
   string globalAccountProgress = DoubleToStr(AccountNumber(), 0) + "Progress";
   double minLot = MarketInfo(Symbol(), MODE_MINLOT);
   double currentBalance = 0;
   string valueComment = "";
   if (!GlobalVariableCheck(globalAccountProgress)) GlobalVariableSet(globalAccountProgress, 0);
   else {
      currentBalance = AccountBalance();
      if (currentBalance > GlobalVariableGet(globalAccountProgress)) GlobalVariableSet(globalAccountProgress, currentBalance);
   }
   if (StartLot < minLot) StartLot = minLot;
   double lastATRHourValue = 0;
   int minBarCount = 48;
   if (iBars(Symbol(), 0) < minBarCount) return (0);
   for (int orderIndex = 0; orderIndex < minBarCount; orderIndex++) {
      atrHourValue = iATR(Symbol(), PERIOD_H1, 14, orderIndex);
      if (atrHourValue > lastATRHourValue) lastATRHourValue = atrHourValue;
   }
   double stopLossFactor = Coef1;
   int calculatedStopLoss = stopLossFactor * MathPow(10, Digits) * lastATRHourValue;
   int normalizeDoubleDigit = 1;
   if (MarketInfo(Symbol(), MODE_MINLOT) == 0.01) normalizeDoubleDigit = 2;
   int pipTarget = calculatedStopLoss / (stopLossFactor * Coef2);
   if (OverideSL > 0) calculatedStopLoss = OverideSL;
   if (OverideTarget > 0) pipTarget = OverideTarget;
   if (DoTrailing) Trailing(Magic, calculatedStopLoss / 3.0);
   for (orderIndex = 0; orderIndex < OrdersTotal(); orderIndex++) {
      OrderSelect(orderIndex, SELECT_BY_POS, MODE_TRADES);
      if (OrderSymbol() == Symbol() && OrderMagicNumber() == Magic) {
         if (OrderType() == OP_BUY) {
            if ((Bid - OrderOpenPrice()) * MathPow(10, Digits) > pipTarget) OrderClose(OrderTicket(), OrderLots(), Bid, 2, Blue);
            else {
               if (OrderType() == OP_SELL)
                  if ((OrderOpenPrice() - Ask) * MathPow(10, Digits) > pipTarget) OrderClose(OrderTicket(), OrderLots(), Ask, 2, Red);
            }
         }
      }
   }
   double rollingProfit = 0;
   double orderLot = StartLot;
   double buySwap = MarketInfo(Symbol(), MODE_SWAPLONG);
   double sellSwap = MarketInfo(Symbol(), MODE_SWAPSHORT);
   
   //orderTrigger; 1 = buy; -1 = sell; 0 = do nothing
   int buyOrSellSignal = 0;
   for (orderIndex = 0; orderIndex < 4; orderIndex++) adxPlusDIArray[orderIndex] = iADX(Symbol(), 0, 14, PRICE_CLOSE, MODE_PLUSDI, orderIndex);
   for (orderIndex = 0; orderIndex < 4; orderIndex++) adxMinusDIArray[orderIndex] = iADX(Symbol(), 0, 14, PRICE_CLOSE, MODE_MINUSDI, orderIndex);
   for (orderIndex = 0; orderIndex < 4; orderIndex++) adxMainArray[orderIndex] = iADX(Symbol(), 0, 14, PRICE_CLOSE, MODE_MAIN, orderIndex);
   if (adxMainArray[2] < adxMainArray[1] && adxMinusDIArray[1] < adxPlusDIArray[1]) buyOrSellSignal = 1;
   else
      if (adxMainArray[2] < adxMainArray[1] && adxMinusDIArray[1] > adxPlusDIArray[1]) buyOrSellSignal = -1;
   if (!Climb) {
      for (orderIndex = 0; orderIndex < OrdersHistoryTotal(); orderIndex++) {
         OrderSelect(orderIndex, SELECT_BY_POS, MODE_HISTORY);
         if (OrderSymbol() == Symbol() && OrderMagicNumber() == Magic) rollingProfit += OrderProfit();
      }
   } else
      if (Climb) rollingProfit = (AccountBalance() - GlobalVariableGet(globalAccountProgress)) / 2.0;
   if (rollingProfit < 0.0) {
      tickValue = MarketInfo(Symbol(), MODE_TICKVALUE);
      orderLot = NormalizeDouble(MathAbs(rollingProfit) / (pipTarget * tickValue), normalizeDoubleDigit);
      if (orderLot == 0.0) orderLot = StartLot;
   }
   if (buyOrSellSignal > 0 && buySwap < 0.0) buyOrSellSignal = 0;
   else
      if (buyOrSellSignal < 0 && sellSwap < 0.0) buyOrSellSignal = 0;
   
   
   int currentOpenOrderCount = 0;
   for (orderIndex = 0; orderIndex < OrdersTotal(); orderIndex++) {
      OrderSelect(orderIndex, SELECT_BY_POS, MODE_TRADES);
      if (OrderSymbol() == Symbol() && OrderMagicNumber() == Magic) currentOpenOrderCount++;
   }
   
   valueComment = valueComment + "BlueIce" 
      + "\n================================" 
      + "\nSettings : " 
      + "\nStartLot : " + DoubleToStr(StartLot, 2) 
      + "\nOverideSL : " + DoubleToStr(OverideSL, 0) + " Pips" 
      + "\nOverideTarget :" + DoubleToStr(OverideTarget, 0) + " Pips" 
      + "\nCoef1 : " + DoubleToStr(Coef1, 2) 
      + "\nCoef2 : " + DoubleToStr(Coef2, 2) 
      + "\nDoTrailing : " + DoTrailing 
      + "\nClimb : " + Climb 
       + "\nTotal Open Orders : " + currentOpenOrderCount
      + "\nMaxSimultaneousOrders : " + DoubleToStr(MaxSimultaneousOrders, 0) 
   + "\n================================";
   Comment(valueComment 
      + "\nCumulative Profit : ", rollingProfit, 
      "\nOrder Lots : ", orderLot, 
      "\nTarget in Pips : ", pipTarget, 
   "\nStop Loss in pips : ", calculatedStopLoss);
   
   int orderOpenBarIndex = -1;
   double buyOrderOpenPrice = 1000;
   double sellOrderOpenPrice = 0;
   if (currentOpenOrderCount > 0) {
      for (orderIndex = OrdersTotal() - 1; orderIndex > 0; orderIndex--) {
         OrderSelect(orderIndex, SELECT_BY_POS, MODE_TRADES);
         if (OrderSymbol() == Symbol() && OrderMagicNumber() == Magic) {
            orderOpenBarIndex = iBarShift(Symbol(), 0, OrderOpenTime(), TRUE);
            break;
         }
      }
      setNewOrderLotFlag = FALSE;
      for (orderIndex = OrdersTotal() - 1; orderIndex > 0; orderIndex--) {
         OrderSelect(orderIndex, SELECT_BY_POS, MODE_TRADES);
         if (OrderSymbol() == Symbol() && OrderMagicNumber() == Magic) {
            if (OrderLots() == orderLot) setNewOrderLotFlag = TRUE;
            if (OrderType() == OP_BUY) {
               if (OrderOpenPrice() < buyOrderOpenPrice) buyOrderOpenPrice = OrderOpenPrice();
            } else {
               if (OrderType() == OP_SELL)
                  if (OrderOpenPrice() > sellOrderOpenPrice) sellOrderOpenPrice = OrderOpenPrice();
            }
         }
      }
      if (setNewOrderLotFlag) orderLot = StartLot;
   }
   if ((orderOpenBarIndex == -1 || orderOpenBarIndex > 10) && currentOpenOrderCount < MaxSimultaneousOrders) {
      if (buyOrSellSignal > 0 && Ask < buyOrderOpenPrice) {OrderSend(Symbol(), OP_BUY, orderLot, Ask, 3, Ask - calculatedStopLoss * Point, 0, "blue", Magic, 0, Blue);Sleep(1000);}
      else
         if (buyOrSellSignal < 0 && Bid > sellOrderOpenPrice) {OrderSend(Symbol(), OP_SELL, orderLot, Bid, 3, Bid + calculatedStopLoss * Point, 0, "blue", Magic, 0, Red);Sleep(1000);}
   }
   return (0);
}
