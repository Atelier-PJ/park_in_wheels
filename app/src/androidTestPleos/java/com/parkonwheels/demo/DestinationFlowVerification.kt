package com.parkonwheels.demo

import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo as Node
import android.graphics.Rect

/** Operates the real installed Pleos activity. No injected model or vehicle signals. */
class DestinationFlowVerification:Instrumentation() {
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    private fun all():List<Node> {
        uiAutomation.clearCache()
        val list=mutableListOf<Node>()
        fun visit(n:Node?){if(n==null)return;list.add(n);for(i in 0 until n.childCount)visit(n.getChild(i))}
        uiAutomation.windows.forEach{visit(it.root)};return list
    }
    private fun find(text:String)=all().firstOrNull{it.text?.toString()?.replace(" ","")==text.replace(" ","")}
    private fun clickNode(node:Node){var n:Node?=node;while(n!=null && !n.isClickable)n=n.parent;check(n?.performAction(Node.ACTION_CLICK)==true);Thread.sleep(450)}
    private fun click(text:String){val end=SystemClock.elapsedRealtime()+10000;while(SystemClock.elapsedRealtime()<end){find(text)?.let{clickNode(it);return};Thread.sleep(200)};error("Missing $text: ${all().mapNotNull{it.text}}")}
    private fun input(value:String){val node=all().firstOrNull{it.isEditable}?:error("No edit field");check(node.performAction(Node.ACTION_SET_TEXT,Bundle().apply{putCharSequence(Node.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value)}));Thread.sleep(350)}
    private fun capture(name:String){Thread.sleep(800);val f=java.io.File(targetContext.filesDir,"destination-flow-$name.png");uiAutomation.takeScreenshot()!!.let{b->f.outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}}}
    private fun parents(){click("보호자");input("14");click("보호자 확인")}
    override fun onStart(){
        val info=uiAutomation.serviceInfo;info.flags=info.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;uiAutomation.serviceInfo=info
        var ok=false;val evidence=StringBuilder()
        try{
            startActivitySync(Intent(targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Thread.sleep(2500)
            find("← 홈으로")?.let{clickNode(it)}
            find("입장하기")?.let{clickNode(it)}
            find("계속하기")?.let{
                clickNode(it);click("이야기 이어서 듣기");click("탐험 끝내기");click("탐험 취소")
            }
            capture("home")
            for((id,title,place) in listOf(Triple("dino","다이노 사파리","서울숲"),Triple("magic","매직 캐슬","롯데월드"),Triple("space","스페이스 포트","국립과천과학관"),Triple("ocean","오션 서브마린","해운대해수욕장"))){
                click(title);capture("$id-transform");click("어디로 가볼까?")
                check(find("오늘은 어디로 가볼까?")!=null);check(find(place)!=null);capture("$id-places")
                click(place);capture("$id-selected")
                val button=find("이야기 시작")?:error("start missing")
                val r=Rect();button.getBoundsInScreen(r)
                if(r.isEmpty || r.bottom>480){all().firstOrNull{it.isScrollable}?.performAction(Node.ACTION_SCROLL_FORWARD);Thread.sleep(400)}
                click("이야기 시작");Thread.sleep(1200);check(find("탐험 끝내기")!=null);capture("$id-story")
                click("탐험 끝내기");click("탐험 취소")
                evidence.append("PASS real Pleos flow $title -> transformation -> destination $place -> story\n")
            }
            ok=true
        }catch(t:Throwable){capture("failure");evidence.append(t.stackTraceToString())}
        finish(if(ok)android.app.Activity.RESULT_OK else android.app.Activity.RESULT_CANCELED,Bundle().apply{putString("stream",evidence.toString())})
    }
}
