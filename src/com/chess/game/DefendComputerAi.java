package com.chess.game;

//防守型机器人
public class DefendComputerAi extends BaseComputerAi {
    @Override
    public Point getBestPoint(){

        Point mostBest = null;

        mostBest = getBestPoint(humanDouble3Alives, computerSencodResults);
        if (mostBest != null) {
            System.out.println("humanDouble3Alives");
            return mostBest;   //这个返回: ①机器人2连+破敌2连 ②机器人2连
        }
        mostBest = getBestPoint(human3Alives, computerSencodResults);
        if (mostBest != null) {
            System.out.println("human3Alives");
            return mostBest;    //总感觉有点重复了,无非也就是返回: ① 机器人3连+破敌3连  ②机器人3连
        }
        mostBest = getBestPoint(humanDouble2Alives, computerSencodResults);
        if (mostBest != null) {
            System.out.println("humanDouble2Alives");
            return mostBest;
        }
        mostBest = getBestPoint(human2Alives, computerSencodResults);
        if (mostBest != null) {
            System.out.println("human2Alives");
            return mostBest;
        }
        mostBest = getBestPoint(human4HalfAlives, computerSencodResults);
        if (mostBest != null) {
            System.out.println("human4HalfAlives");
            return mostBest;
        }
        mostBest = getBestPoint(human3HalfAlives, computerSencodResults);
        if (mostBest != null) {
            System.out.println("human3HalfAlives");
            return mostBest;
        }

        mostBest = getBestPoint(computerDouble2Alives, humanSencodResults);
        if (mostBest != null) {
            System.out.println("computerDouble2Alives");
            return mostBest;   //总感觉有点重复了,无非也就是返回: ① 机器人2连+破敌2连  ②机器人2连
        }
        mostBest = getBestPoint(computer2Alives, humanSencodResults);
        if (mostBest != null) {
            System.out.println("computer2Alives");
            return mostBest;
        }
        //即将单活4，且我没有半活4以上的，只能堵
        mostBest = getBestPoint(computerDouble3Alives, humanSencodResults);
        if (mostBest != null) {
            System.out.println("computerDouble3Alives");
            return mostBest;    //这个返回: ① 机器人3连+破敌3连  ②机器人3连
        }
        mostBest = getBestPoint(computer3Alives, humanSencodResults);
        if (mostBest != null) {
            System.out.println("computer3Alives");
            return mostBest;  //这个返回: ① 机器人3连+破敌3连  ②机器人3连
        }
        mostBest = getBestPoint(computer3HalfAlives, humanSencodResults);
            System.out.println("computer3HalfAlives");
            return  mostBest;
    }
}
