package com.chess.game;

import java.util.*;

//算法核心类，算法的主体思想分三个步骤，
//第一步：根据双方的当前的形势循环地假设性的分别给自己和对方下一子（在某个范围内下子），并判断此棋子能带来的形势上的变化，如能不能冲4，能不能形成我方或敌方双3等，
//第二步：根据上一步结果，组合每一步棋子所带来的所有结果（如某一步棋子可能形成我方1个活3，1个冲4（我叫它半活4）等），包括敌方和我方的。
//第三步：根据用户给的规则对上一步结果进行排序，并选子（有进攻形、防守形规则）
public class BaseComputerAi extends BasePlayer {


    // 四个方向，横- 、纵| 、正斜/ 、反斜\
    private static final int HENG = 0;
    private static final int ZHONG = 1;
    private static final int ZHENG_XIE = 2;
    private static final int FAN_XIE = 3;
    //往前往后
    private static final boolean FORWARD = true;
    private static final boolean BACKWARD = false;


    //标示分析结果当前点位是两头通（ALIVE）还是只有一头通（HALF_ALIVE），封死的棋子分析过程自动屏蔽，不作为待选棋子
    private static final int ALIVE = 1;
    private static final int HALF_ALIVE = 0;

    public BaseComputerAi() {
        humanSencodResults = new ArrayList<SencondAnalysisResult>();
    }


    //private static final int DEAD = -1;

    //计算范围，太大的范围会有性能问题
    private class CalcuteRange {
        int xStart, yStart, xStop, yStop;

        private CalcuteRange(int xStart, int yStart, int xStop, int yStop) {
            this.xStart = xStart;
            this.yStart = yStart;
            this.xStop = xStop;
            this.yStop = yStop;
        }
    }


    //限定电脑计算范围，如果整个棋盘计算，性能太差，目前是根据所有已下的棋子的边界值加RANGE_STEP值形成，目前为1
    private static final int RANGE_STEP = 1;
    CalcuteRange currentRange = new CalcuteRange(0, 0, 0, 0);

    private void initRange(List<Point> comuters, List<Point> humans) {
        currentRange.xStart = humans.get(0).getX() - RANGE_STEP;
        currentRange.yStart = humans.get(0).getY() - RANGE_STEP;
        currentRange.xStop = humans.get(0).getX() + RANGE_STEP;
        currentRange.yStop = humans.get(0).getY() + RANGE_STEP;
        for (Point point : humans) {
            if (point.getX() - RANGE_STEP < currentRange.xStart) {
                currentRange.xStart = point.getX() - RANGE_STEP;
            } else if (point.getX() + RANGE_STEP > currentRange.xStop) {
                currentRange.xStop = point.getX() + RANGE_STEP;
            }
            if (point.getY() - RANGE_STEP < currentRange.yStart) {
                currentRange.yStart = point.getY() - RANGE_STEP;
            } else if (point.getY() + RANGE_STEP > currentRange.yStop) {
                currentRange.yStop = point.getY() + RANGE_STEP;
            }
        }
        for (Point point : comuters) {
            if (point.getX() - RANGE_STEP < currentRange.xStart) {
                currentRange.xStart = point.getX() - RANGE_STEP;
            } else if (point.getX() + RANGE_STEP > currentRange.xStop) {
                currentRange.xStop = point.getX() + RANGE_STEP;
            }
            if (point.getY() - RANGE_STEP < currentRange.yStart) {
                currentRange.yStart = point.getY() - RANGE_STEP;
            } else if (point.getY() + RANGE_STEP > currentRange.yStop) {
                currentRange.yStop = point.getY() + RANGE_STEP;
            }
        }

        //如果范围扩大后超过了棋盘，则等于棋盘
        currentRange.xStart = currentRange.xStart < 0 ? 0 : currentRange.xStart;
        currentRange.yStart = currentRange.yStart < 0 ? 0 : currentRange.yStart;
        currentRange.xStop = currentRange.xStop >= maxX ? maxX - 1 : currentRange.xStop;
        currentRange.yStop = currentRange.yStop >= maxY ? maxY - 1 : currentRange.yStop;
    }

    // 分析当前形式的入口方法，分析总共分三个步骤，第三步骤可由子类干预以作难度控制
    private Point doAnalysis(List<Point> comuters, List<Point> humans) {
        if (humans.size() == 1) {//第一步
            return getFirstPoint(humans);
        }

        //初始化计算范围
        initRange(comuters, humans);

        //清除以前的结果
        initAnalysisResults();
        // 开始分析，扫描所有空白点，形成第一次分析结果
        Point bestPoint = doFirstAnalysis(comuters, humans);
        if (bestPoint != null) {
            System.out.println("这个棋子最重要，只能下这个棋子,这个棋子要吗是机器人的五连绝杀,要吗是机器人封堵人类的单向4连的最后一子绝杀");
            return bestPoint;
        }
        // 分析第一次结果，找到自己的最佳点位
        bestPoint = doComputerSencondAnalysis(computerFirstResults, computerSencodResults);
        if (bestPoint != null) {
            System.out.println("快要赢了，就下这个棋子,这个棋子将会形成机器人双活4的决胜棋面!!!");
            return bestPoint;
        }
        computerFirstResults.clear();
        System.gc();
        // 分析第一次结果，找到敌人的最佳点位,有点问题,下面这个方法根本就不会有返回值
        //这个bestPoint永远都是null,但是其实3轮中又会分析一次,把这种情况封堵住
        bestPoint = doHumanSencondAnalysis(humanFirstResults, humanSencodResults);
        if (bestPoint != null) {
            System.out.println("再不下这个棋子就输了,这个棋子将会拆掉人类即将成型的双活4连?????搞笑吗,这里能进来???");
            return bestPoint;
        }
        humanFirstResults.clear();
        System.gc();
        //没找到绝杀点，第三次结果分析
        return doThirdAnalysis();
    }


    private static final HashMap<Integer, Integer> fMap = new HashMap<>();


    //BUG修复：当电脑是黑棋的时候，白棋下在黑棋右边一颗（即开局黑(7,7)白(7,8)）会出现bug.
    //下第一步棋子，不需要复杂的计算，根据人类第一步棋子X值减1完成
    private Point getFirstPoint(List<Point> humans) {
        Point point = humans.get(0);
        if (myPoints.isEmpty()) {
            //人类已经下了一颗，我还没有下，所以人类是先手
            return xm1(point);
        } else if (point.x == 6 && point.y == 6) {
            return new Point(6, 8);
        } else if (point.x == 6 && point.y == 7) {
            return new Point(6, 6);
        } else if (point.x == 6 && point.y == 8) {
            return new Point(6, 6);
        } else if (point.x == 7 && point.y == 6) {
            return new Point(8, 8);
        } else if (point.x == 7 && point.y == 8) {
            return new Point(6, 8);
        } else if (point.x == 8 && point.y == 6) {
            return new Point(6, 6);
        } else if (point.x == 8 && point.y == 7) {
            return new Point(6, 6);
        } else if (point.x == 8 && point.y == 8) {
            return new Point(6, 8);
        } else if (point.x == 5 && point.y == 5) {
            return new Point(6, 8);
        } else {
            return new Point(6, 6);
        }


    }

    private Point xm1(Point point) {
        if (point.getX() == 0 || point.getY() == 0 || point.getX() == maxX && point.getY() == maxY)
            return new Point(maxX / 2, maxY / 2);
        else {
            return new Point(point.getX() - 1, point.getY());
        }
    }

//  private int debugx,debugy;//用于DEBUG

    // 开始分析，扫描所有空白点，形成第一次分析结果
    private Point doFirstAnalysis(List<Point> comuters, List<Point> humans) {
        int size = allFreePoints.size();
        Point computerPoint = null;
        Point humanPoint = null;
        int x, y;
        FirstAnalysisResult firstAnalysisResult;
        for (int i = 0; i < size; i++) {
            computerPoint = allFreePoints.get(i);
            //先把X、Y坐标记下来，因为在分析过程中会改变原来的对象
            x = computerPoint.getX();
            y = computerPoint.getY();
            if (x < currentRange.xStart || x > currentRange.xStop || y < currentRange.yStart || y > currentRange.yStop) {
                continue;
            }

//          if(x==debugx && y==debugy){
//              System.out.println("sssssssssssss");
//          }

            //尝试在此位置上下一个棋子，并分析在“横向”这个方向上我方可形成的状态，如活4，活3，半活4，活2等所有状态
            firstAnalysisResult = tryAndCountResult(comuters, humans, computerPoint, HENG);
            computerPoint.setX(x).setY(y);//回复点位的原值，以供下次分析
            if (firstAnalysisResult != null) {//无返回结果此方向上不可能达到五个棋子，
                if (firstAnalysisResult.count == 5)//等于5表示在此点上下棋子即可连成5个，胜利了，不再往下进行分析
                    return computerPoint;
                //记录第一次分析结果
                addToFirstAnalysisResult(firstAnalysisResult, computerFirstResults);
            }

            //在“纵向”这个方向上重复上面的步骤
            firstAnalysisResult = tryAndCountResult(comuters, humans, computerPoint, ZHONG);
            computerPoint.setX(x).setY(y);
            if (firstAnalysisResult != null) {//死棋，不下
                if (firstAnalysisResult.count == 5)
                    return computerPoint;

                addToFirstAnalysisResult(firstAnalysisResult, computerFirstResults);
            }

            //正斜向
            firstAnalysisResult = tryAndCountResult(comuters, humans, computerPoint, ZHENG_XIE);
            computerPoint.setX(x).setY(y);
            if (firstAnalysisResult != null) {//死棋，不下
                if (firstAnalysisResult.count == 5)
                    return computerPoint;

                addToFirstAnalysisResult(firstAnalysisResult, computerFirstResults);
            }

            //反斜向
            firstAnalysisResult = tryAndCountResult(comuters, humans, computerPoint, FAN_XIE);
            computerPoint.setX(x).setY(y);
            if (firstAnalysisResult != null) {//死棋，不下
                if (firstAnalysisResult.count == 5)
                    return computerPoint;

                addToFirstAnalysisResult(firstAnalysisResult, computerFirstResults);
            }

            //在“横向”上分析此棋子可在敌方形成如何状态，如敌方的活3、半活4等
            //后面这四条为什么要计算人类玩家的落子:计算后如果发现玩家有高威胁的双活3,即将双活4的杀招,则立即封死玩家的落子
            firstAnalysisResult = tryAndCountResult(humans, comuters, computerPoint, HENG);
            computerPoint.setX(x).setY(y);
            if (firstAnalysisResult != null) {//死棋，不下
                if (firstAnalysisResult.count == 5)
                    humanPoint = computerPoint;

                addToFirstAnalysisResult(firstAnalysisResult, humanFirstResults);
            }

            //“纵向”
            firstAnalysisResult = tryAndCountResult(humans, comuters, computerPoint, ZHONG);
            computerPoint.setX(x).setY(y);
            if (firstAnalysisResult != null) {//死棋，不下
                if (firstAnalysisResult.count == 5)
                    humanPoint = computerPoint;

                addToFirstAnalysisResult(firstAnalysisResult, humanFirstResults);
            }

            //“正斜”
            firstAnalysisResult = tryAndCountResult(humans, comuters, computerPoint, ZHENG_XIE);
            computerPoint.setX(x).setY(y);
            if (firstAnalysisResult != null) {//死棋，不下
                if (firstAnalysisResult.count == 5)
                    humanPoint = computerPoint;

                addToFirstAnalysisResult(firstAnalysisResult, humanFirstResults);
            }

            //“反斜”
            firstAnalysisResult = tryAndCountResult(humans, comuters, computerPoint, FAN_XIE);
            computerPoint.setX(x).setY(y);
            if (firstAnalysisResult != null) {//死棋，不下
                if (firstAnalysisResult.count == 5)
                    humanPoint = computerPoint;

                addToFirstAnalysisResult(firstAnalysisResult, humanFirstResults);
            }
        }
        //如果没有绝杀棋子，第一次分析不需要返回结果
        return humanPoint;
    }

    //第二次分析，分析第一次形成的结果，第一次分析结果会把一步棋在四个方向上可形成的结果生成最多四个FirstAnalysisResult对象（敌我各四）
    //这里要把这四个对象组合成一个SencondAnalysisResult对象，
    private Point doComputerSencondAnalysis(Map<Point, List<FirstAnalysisResult>> firstResults, List<SencondAnalysisResult> sencodResults) {
        List<FirstAnalysisResult> list = null;
        SencondAnalysisResult second = null;
        for (Point p : firstResults.keySet()) {
            second = new SencondAnalysisResult(p);
            list = firstResults.get(p);
            for (FirstAnalysisResult result : list) {
                if (result.count == 4) {
                    if (result.aliveState == ALIVE) {//经过前面的过滤，双方都排除了绝杀棋，有活4就下这一步了，再下一步就赢了
                        return result.point;//如果有绝杀，第一轮已返回，在此轮活4已经是好的棋子，直接返回，不再往下分析
                    } else {
                        second.halfAlive4++;   //如果是半活4的话,虽然也还凑活,但不会被立即返回了,留待后续进一步分析
                        computer4HalfAlives.add(second);
                    }
                } else if (result.count == 3) {
                    if (result.aliveState == ALIVE) {
                        second.alive3++;     //双活3
                        if (second.alive3 == 1) {
                            computer3Alives.add(second);
                        } else {
                            computerDouble3Alives.add(second);   //这里没懂为什么都是双活3要分开存放
                        }
                    } else {
                        second.halfAlive3++;
                        computer3HalfAlives.add(second);   //半活3
                    }
                } else {//半活2在第一阶段已被排除，不再处理,所以这里进行记录的其实是双活2
                    second.alive2++;
                    if (second.alive2 == 1) {
                        computer2Alives.add(second);
                    } else {
                        computerDouble2Alives.add(second);
                    }
                }
            }
            sencodResults.add(second);
        }
        //没有找到活4
        return null;
    }

    //这个方法和上面的基本一样，但为了性能，少作几次判断，将人类和电脑的分开了
    private Point doHumanSencondAnalysis(Map<Point, List<FirstAnalysisResult>> firstResults, List<SencondAnalysisResult> sencodResults) {
        List<FirstAnalysisResult> list = null;
        SencondAnalysisResult second = null;
        for (Point p : firstResults.keySet()) {
            second = new SencondAnalysisResult(p);
            list = firstResults.get(p);
            for (FirstAnalysisResult result : list) {
                if (result.count == 4) {
                    if (result.aliveState == ALIVE) {  //双活4连
                        human4Alives.add(second);
                    } else {  //非四连的情形
                        second.halfAlive4++;        //单活4连
                        human4HalfAlives.add(second);
                    }
                } else if (result.count == 3) {
                    if (result.aliveState == ALIVE) {
                        second.alive3++;         //双活3连
                        if (second.alive3 == 1) {
                            human3Alives.add(second);
                        } else {
                            humanDouble3Alives.add(second);
                        }
                    } else {
                        second.halfAlive3++;
                        human3HalfAlives.add(second);
                    }
                } else {
                    second.alive2++;
                    if (second.alive2 == 1) {
                        human2Alives.add(second);
                    } else {
                        humanDouble2Alives.add(second);
                    }
                }
            }
            sencodResults.add(second);
        }
        //没有找到活4
        return null;
    }

    private void sleep(int miniSecond) {
        try {
            Thread.sleep(miniSecond);
        } catch (InterruptedException e) {
        }
    }


    //第三次分析，双方都不可以制造活4，找双活3棋子，不行就找半活4，再不行就找单活3，双活2
    private Point doThirdAnalysis() {
        if (!computer4HalfAlives.isEmpty()) {   //半活4的优先级最高
            System.out.println("半活4连,第三高的优先级,就下这个了");
            return computer4HalfAlives.get(0).point;
        }
        System.gc();
        sleep(300);
        Collections.sort(computerSencodResults);   //请问这个排序的结果是啥?
        System.gc();

        //即将单活4，且我没有半活4以上的，只能堵
        Point mostBest = getBestPoint(human4Alives, computerSencodResults);
        if (mostBest != null) {
            System.out.println("这个mostbest,要吗是二轮目标棋子且可以打断玩家的双活3连,要吗就是虽然非二轮但是可以打断玩家双活3连");
            return mostBest;
        }

        Collections.sort(humanSencodResults);  //请问这个排序的结果又是啥????
        System.gc();

        mostBest = getBestPoint();  //注意这个getBestPoint()是个重载的方法,这个方法不带参数,就纯属怎么选结果都可以了
        if (mostBest != null)
            return mostBest;

        //拿出各自排第一的，谁好就下谁,实在选不出来必须要下的棋子了,就只能这样了
        System.out.println("重载getBestPoint没有找到好棋");
        return computerSencodResults.get(0).point;
    }

    //子类实现这个方法，并改变其顺序可以实现防守为主还是猛攻,这个逻辑其实是防守为主吧
    protected Point getBestPoint() {
       throw new RuntimeException("子类必须覆盖这个方法!!!");
    }

    //第三次分析的最后一步，第二次结果已经过排序，在此可以从前面选出最好的棋子  //第一个参数传人类的双活4,第二个传机器人二轮分析
    protected Point getBestPoint(List<SencondAnalysisResult> myBest, List<SencondAnalysisResult> yourSencodResults) {
        if (!myBest.isEmpty()) {
            if (myBest.size() > 1) {
                for (SencondAnalysisResult your : yourSencodResults) {
                    if (myBest.contains(your)) {   //如果机器人的二轮分析的结果中有这么个棋子:这个棋子可以破坏人类的双活4连,而且是二轮的理想棋子,那么这个棋子的优先级最高
                        return your.point;
                    }
                }
                return myBest.get(0).point;   //这个棋子虽然不是二轮中的理想目标,但是它可以破坏人类的四连,只能这么下了,否则要输
            } else {
                return myBest.get(0).point;  //这个棋子虽然不是二轮中的理想目标,但是它可以破坏人类的四连,只能这么下了,否则要输
            }
        }
        return null;   //人类并没有可以形成双活四连的棋子,那就继续往下筛
    }


    //第一次分析结果
    private final Map<Point, List<FirstAnalysisResult>> computerFirstResults = new HashMap<>();
    private final Map<Point, List<FirstAnalysisResult>> humanFirstResults = new HashMap<>();
    //第二次总结果
    protected final List<SencondAnalysisResult> computerSencodResults = new ArrayList<>();
    protected final List<SencondAnalysisResult> humanSencodResults;
    //第二次分结果，电脑
    protected final List<SencondAnalysisResult> computer4HalfAlives = new ArrayList<>(2);
    protected final List<SencondAnalysisResult> computerDouble3Alives = new ArrayList<>(4);
    protected final List<SencondAnalysisResult> computer3Alives = new ArrayList<>(5);
    protected final List<SencondAnalysisResult> computerDouble2Alives = new ArrayList<>();
    protected final List<SencondAnalysisResult> computer2Alives = new ArrayList<>();
    protected final List<SencondAnalysisResult> computer3HalfAlives = new ArrayList<>();

    //第二次分结果，人类
    protected final List<SencondAnalysisResult> human4Alives = new ArrayList<>(2);         //双活4
    protected final List<SencondAnalysisResult> human4HalfAlives = new ArrayList<>(5);
    protected final List<SencondAnalysisResult> humanDouble3Alives = new ArrayList<>(2);
    protected final List<SencondAnalysisResult> human3Alives = new ArrayList<>(10);
    protected final List<SencondAnalysisResult> humanDouble2Alives = new ArrayList<>(3);
    protected final List<SencondAnalysisResult> human2Alives = new ArrayList<>();
    protected final List<SencondAnalysisResult> human3HalfAlives = new ArrayList<>();

    //第一次分析前清空上一步棋子的分析结果
    private void initAnalysisResults() {
        computerFirstResults.clear();
        humanFirstResults.clear();
        //第二次总结果
        computerSencodResults.clear();
        humanSencodResults.clear();
        //第二次分结果
        computer4HalfAlives.clear();
        computerDouble3Alives.clear();
        computer3Alives.clear();
        computerDouble2Alives.clear();
        computer2Alives.clear();
        computer3HalfAlives.clear();

        //第二次分结果，人类
        human4Alives.clear();
        human4HalfAlives.clear();
        humanDouble3Alives.clear();
        human3Alives.clear();
        humanDouble2Alives.clear();
        human2Alives.clear();
        human3HalfAlives.clear();
        System.gc();
    }

    //加入到第一次分析结果中
    private void addToFirstAnalysisResult(FirstAnalysisResult result, Map<Point, List<FirstAnalysisResult>> dest) {
        if (dest.containsKey(result.point)) {
            dest.get(result.point).add(result);
        } else {
            List<FirstAnalysisResult> list = new ArrayList<>(1);
            list.add(result);
            dest.put(result.point, list);
        }
    }


    //第一次分析结果类
    private class FirstAnalysisResult {
        //连续数,count等于5的时候代表5连了,即获得了胜利
        int count;
        //点位
        Point point;
        //方向
        int direction;
        //状态,这个状态一直不是很懂什么意思
        int aliveState;

        private FirstAnalysisResult(int count, Point point, int direction) {
            this(count, point, direction, ALIVE);
        }

        private FirstAnalysisResult(int count, Point point, int direction, int aliveState) {
            this.count = count;
            this.point = point;
            this.direction = direction;
            this.aliveState = aliveState;
        }


        private FirstAnalysisResult init(Point point, int direction, int aliveState) {
            this.count = 1;
            this.point = point;
            this.direction = direction;
            this.aliveState = aliveState;
            return this;
        }

        private FirstAnalysisResult cloneMe() {
            return new FirstAnalysisResult(count, point, direction, aliveState);
        }

    }

    //第二次分析结果类
    class SencondAnalysisResult implements Comparable<SencondAnalysisResult> {
        int alive4 = 0;
        //活3数量
        int alive3 = 0;
        //半活4，一头封的
        int halfAlive4 = 0;
        //半活3，一头封的
        int halfAlive3 = 0;
        //活2数量
        int alive2 = 0;
        //点位
        Point point;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((point == null) ? 0 : point.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            SencondAnalysisResult other = (SencondAnalysisResult) obj;
            if (point == null) {
                if (other.point != null)
                    return false;
            } else if (!point.equals(other.point))
                return false;
            return true;
        }

        private SencondAnalysisResult(Point point) {
            this.point = point;
        }


        //第三次分析时，对第二次分析结果进行排序，此为排序回调函数
        @Override
        public int compareTo(SencondAnalysisResult another) {
            return compareTowResult(this, another);
        }

    }

    //返加-1则第一个参数优先，1则第二个参数优先，0则按原来顺序
    private int compareTowResult(SencondAnalysisResult oneResult, SencondAnalysisResult another) {
        if (oneResult.alive4 > another.alive4) {
            return -1;
        }
        if (oneResult.alive4 < another.alive4) {
            return 1;
        }
        if (oneResult.halfAlive4 > another.halfAlive4) {
            return -1;
        }
        if (oneResult.halfAlive4 < another.halfAlive4) {
            return 1;
        }
        if (oneResult.alive3 > another.alive3) {
            return -1;
        }
        if (oneResult.alive3 < another.alive3) {
            return 1;
        }
        if (oneResult.alive2 > another.alive2) {
            return -1;
        }
        if (oneResult.alive2 < another.alive2) {
            return 1;
        }
        if (oneResult.halfAlive3 > another.halfAlive3) {
            return -1;
        }
        if (oneResult.halfAlive3 > another.halfAlive3) {
            return 1;
        }
        return 0;
    }


    //一个临时对象，供第一次分析时临时存放分析结果使用，如果分析出有活1以上（不含）的结果，则调用其cloneMe方法获得结果，否则抛弃此结果
    private final FirstAnalysisResult far = new FirstAnalysisResult(1, null, HENG);

    // 分析如果在当前位下一子，会形成某个方向上多少个子，参数：当前己方已下的所有点，当前要假设的点，需要判断的方向
    private FirstAnalysisResult tryAndCountResult(List<Point> myPoints, List<Point> enemyPoints, Point point, int direction) {
        int x = point.getX();
        int y = point.getY();
        FirstAnalysisResult fr = null;

        //maxCountOnThisDirection这个方法其实是在试探:该落子在一个方向比如横向上有没有可能出现5连,连这个都保证不了,这个子的价值是非常低的
        int maxCountOnThisDirection = maxCountOnThisDirection(point, enemyPoints, direction, 1);
        if (maxCountOnThisDirection < 5) {
            //无意义的棋子
            return null;//此方向不足五个空位，已排除己方已下的棋子,这个方向最多只能落不到5个子,无论如何都赢不了,应直接忽略
        } else if (maxCountOnThisDirection == 5) {
            //半死状态，当是一头通,出现这种情况,也就是这个方向刚好还能落下五个子,比如两端都是敌子,中间5空位,或者一端敌,一端顶到了边线
            fr = far.init(point, direction, HALF_ALIVE);
        } else {
            //两头皆通,这种就是一个方向空间非常大,怎么下都行,一轮分析的时候,这种落点都是可以考略的
            fr = far.init(point, direction, ALIVE);
        }

        //在前和后的方向上计算一次
        countPoint(myPoints, enemyPoints, point.setX(x).setY(y), fr, direction, FORWARD);
        countPoint(myPoints, enemyPoints, point.setX(x).setY(y), fr, direction, BACKWARD);  //换一个方向再尝试一下,说不定另外一个方向又是另一番风景


        if (fr.count <= 1 || (fr.count == 2 && fr.aliveState == HALF_ALIVE)) {//活1，半活2及其以下结果，抛弃,为什么要抛弃2连的半活????
            return null;
        }
        //返回复制的结果
        return fr.cloneMe();
    }

    //棋子出了墙
    private boolean isOutSideOfWall(Point point, int direction) {
        if (direction == HENG) {
            return point.getX() < 0 || point.getX() >= maxX;//最大的X和Y值均在墙外所以用等号
        } else if (direction == ZHONG) {
            return point.getY() < 0 || point.getY() >= maxY;
        } else {//这里可能有问题
            return point.getX() < 0 || point.getY() < 0 || point.getX() >= maxX || point.getY() >= maxY;
        }
    }
    //就是计算传入的棋子按照指定direction和forwar走一步后的落点
    private Point pointToNext(Point point, int direction, boolean forward) {
        switch (direction) {
            case HENG:
                if (forward)
                    point.x++;
                else
                    point.x--;
                break;
            case ZHONG:
                if (forward)
                    point.y++;
                else
                    point.y--;
                break;
            case ZHENG_XIE:
                if (forward) {
                    point.x++;
                    point.y--;
                } else {
                    point.x--;
                    point.y++;
                }
                break;
            case FAN_XIE:
                if (forward) {
                    point.x++;
                    point.y++;
                } else {
                    point.x--;
                    point.y--;
                }
                break;
        }
        return point;
    }

    //在某个方向（八个中的一个）可下多少棋子，这个方法是第一分析中的核心方法
    private void countPoint(List<Point> myPoints, List<Point> enemyPoints, Point point, FirstAnalysisResult fr, int direction, boolean forward) {
        if (myPoints.contains(pointToNext(point, direction, forward))) {
            fr.count++;   //出现了2连了,fr的连击数字加1
            if (myPoints.contains(pointToNext(point, direction, forward))) {
                fr.count++;  //出现了3连
                if (myPoints.contains(pointToNext(point, direction, forward))) {
                    fr.count++; //出现了4连
                    if (myPoints.contains(pointToNext(point, direction, forward))) {
                        fr.count++;  //出现了5连,这个属于杀招了吧
                    } else if (enemyPoints.contains(point) || isOutSideOfWall(point, direction)) {
                        fr.aliveState = HALF_ALIVE;  //很遗憾,虽然四连了,但是该方向五连的位置已经被堵死了,别绝望,这个方法会进来两次,我们还会尝试另外一个方向
                    }
                } else if (enemyPoints.contains(point) || isOutSideOfWall(point, direction)) {
                    fr.aliveState = HALF_ALIVE;
                }
            } else if (enemyPoints.contains(point) || isOutSideOfWall(point, direction)) {
                fr.aliveState = HALF_ALIVE;
            }
        } else if (enemyPoints.contains(point) || isOutSideOfWall(point, direction)) {
            fr.aliveState = HALF_ALIVE;
        }
    }


    //在某个方向上是否还能下到满五个棋子
    private int maxCountOnThisDirection(Point point, List<Point> enemyPoints, int direction, int count) {
        int x = point.getX(), y = point.getY();
        switch (direction) {
            //横向
            case HENG:
                while (!enemyPoints.contains(point.setX(point.getX() - 1)) && point.getX() >= 0 && count < 6) {
                    count++;
                }
                point.setX(x);
                while (!enemyPoints.contains(point.setX(point.getX() + 1)) && point.getX() < maxX && count < 6) {
                    count++;
                }
                break;
            //纵向
            case ZHONG:
                while (!enemyPoints.contains(point.setY(point.getY() - 1)) && point.getY() >= 0) {
                    count++;
                }
                point.setY(y);
                while (!enemyPoints.contains(point.setY(point.getY() + 1)) && point.getY() < maxY && count < 6) {
                    count++;
                }
                break;
            //正斜向 /
            case ZHENG_XIE:
                while (!enemyPoints.contains(point.setX(point.getX() - 1).setY(point.getY() + 1)) && point.getX() >= 0 && point.getY() < maxY) {
                    count++;
                }
                point.setX(x).setY(y);
                while (!enemyPoints.contains(point.setX(point.getX() + 1).setY(point.getY() - 1)) && point.getX() < maxX && point.getY() >= 0 && count < 6) {
                    count++;
                }
                break;
            //反斜 /
            case FAN_XIE:
                while (!enemyPoints.contains(point.setX(point.getX() - 1).setY(point.getY() - 1)) && point.getX() >= 0 && point.getY() >= 0) {
                    count++;
                }
                point.setX(x).setY(y);
                while (!enemyPoints.contains(point.setX(point.getX() + 1).setY(point.getY() + 1)) && point.getX() < maxX && point.getY() < maxY && count < 6) {
                    count++;
                }
                break;
        }
        return count;
    }

    //下棋子，对外接口
    @Override
    public void run(List<Point> humans, Point p) {
        //把人类下的最后一步棋子去除
        allFreePoints.remove(humans.get(humans.size() - 1));
        //电脑可以下的一步棋子
        Point result = null;
        try {
            result = doAnalysis(myPoints, humans);
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            //修复可能出现的罕见bug：当快下满的时候（和棋时）可能出现AI崩溃的bug，暂定的解决方式是：AI随机下子。
            Random random = new Random();
            int i = random.nextInt(allFreePoints.size());
            result = allFreePoints.get(i);
        }
        //去除电脑下的棋子

        allFreePoints.remove(result);
        //加入到电脑棋子中，下棋了
        myPoints.add(result);
    }
}