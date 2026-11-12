package com.parkonwheels.demo

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

private val Ink = Color(0xFF24394A)
private val Cream = Color(0xFFFFFCF1)

/** Original resolution-independent illustrations, packaged with the app and available offline. */
@Composable
fun ThemeArtwork(id: String, modifier: Modifier = Modifier, celebration: Boolean = false) {
    Canvas(modifier) {
        val factor = minOf(size.width / 600f, size.height / 440f)
        withTransform({ translate((size.width - 600 * factor) / 2, (size.height - 440 * factor) / 2); scale(factor, factor, Offset.Zero) }) {
            when (id) { "magic" -> castle(); "space" -> space(); "ocean" -> ocean(); else -> dinosaur(celebration) }
            if (celebration) for (i in 0..12) {
                val x = 55f + (i * 47 % 500); val y = 42f + (i * 59 % 320)
                star(x, y, 7f, listOf(Color(0xFFFFC857), Color(0xFFEEA3AF), Color(0xFF83C4B2))[i % 3])
            }
        }
    }
}
private fun DrawScope.oval(c: Color, x: Float, y: Float, w: Float, h: Float) = drawOval(c, Offset(x,y), Size(w,h))
private fun DrawScope.round(c: Color, x: Float,y: Float,w: Float,h: Float,r: Float=24f) = drawRoundRect(c,Offset(x,y),Size(w,h),CornerRadius(r))
private fun DrawScope.eye(x: Float,y: Float) { drawCircle(Ink,7f,Offset(x,y)); drawCircle(Color.White,2f,Offset(x+2,y-2)) }
private fun DrawScope.star(x:Float,y:Float,r:Float,c:Color) {
    val p=Path(); for (i in 0..9) { val a=(i*36-90)*Math.PI/180; val d=if(i%2==0)r else r*.46f; val px=x+kotlin.math.cos(a).toFloat()*d;val py=y+kotlin.math.sin(a).toFloat()*d;if(i==0)p.moveTo(px,py)else p.lineTo(px,py) };p.close();drawPath(p,c)
}
private fun DrawScope.leaf(x:Float,y:Float,mirror:Boolean=false) {
    val p=Path().apply { moveTo(x,y+65);cubicTo(x-65,y+10,x-36,y-36,x,y);cubicTo(x+45,y-35,x+65,y+15,x,y+65);close() }
    drawPath(p,Color(0xFF539E70));drawLine(Color(0xFFAED1A2),Offset(x,y+60),Offset(x,y),4f)
}
private fun DrawScope.dinosaur(hatched:Boolean) {
    oval(Color(0xFFD0E7C3),70f,338f,470f,66f)
    leaf(100f,265f);leaf(485f,275f)
    // Friendly explorer dinosaur, with a scarf and a softly spotted egg.
    val tail=Path().apply {moveTo(220f,292f);cubicTo(120f,280f,155f,217f,113f,236f);cubicTo(100f,315f,146f,351f,235f,352f);close()};drawPath(tail,Color(0xFF69AA73))
    oval(Color(0xFF7CB97E),192f,200f,205f,166f)
    round(Color(0xFF68A96F),209f,321f,57f,49f,21f);round(Color(0xFF68A96F),318f,321f,57f,49f,21f)
    oval(Color(0xFFB4D698),245f,241f,125f,98f)
    oval(Color(0xFF86C28B),233f,92f,162f,153f)
    oval(Color(0xFF86C28B),310f,151f,123f,83f)
    for(i in 0..2)oval(Color(0xFF4E966A),235f+i*11,119f+i*30,24f,24f)
    eye(337f,150f);drawCircle(Color(0xFFDE9B9D),12f,Offset(370f,181f))
    drawArc(Ink,10f,70f,false,Offset(374f,177f),Size(31f,24f),style=Stroke(3f))
    round(Color(0xFFF5BD64),264f,219f,139f,19f,9f)
    val scarf=Path().apply{moveTo(310f,228f);lineTo(340f,268f);lineTo(358f,244f);lineTo(339f,224f);close()};drawPath(scarf,Color(0xFFF5BD64))
    round(Color(0xFF8BC38D),342f,268f,67f,26f,13f)
    if (!hatched) {
        val egg=Path().apply{moveTo(446f,227f);cubicTo(405f,228f,372f,322f,397f,351f);cubicTo(422f,380f,492f,365f,501f,335f);cubicTo(509f,300f,477f,225f,446f,227f);close()};drawPath(egg,Cream)
        oval(Color(0xFFADCE9D),422f,252f,23f,34f);oval(Color(0xFFADCE9D),462f,307f,23f,31f);oval(Color(0xFFADCE9D),405f,319f,20f,22f)
    } else { star(448f,280f,42f,Color(0xFFFFCA63));star(445f,275f,19f,Cream) }
    star(174f,113f,19f,Color(0xFFF6C461));star(460f,132f,13f,Color(0xFFF6C461))
    oval(Color.White.copy(.6f),118f,58f,91f,25f)
}
private fun DrawScope.castle() {
    oval(Color(0xFFD8C9EC),70f,351f,475f,45f)
    drawCircle(Color(0xFFF8D46D),40f,Offset(457f,87f));drawCircle(Color(0xFFF0E7FA),35f,Offset(473f,74f))
    round(Color(0xFFBAA0D9),153f,180f,294f,183f,12f)
    round(Color(0xFFD2BFEA),130f,149f,91f,214f,14f);round(Color(0xFFD2BFEA),385f,149f,91f,214f,14f)
    round(Color(0xFFEADDF4),255f,114f,94f,249f,12f)
    fun roof(x:Float,y:Float,w:Float){val p=Path().apply{moveTo(x-w/2,y+70);lineTo(x,y);lineTo(x+w/2,y+70);close()};drawPath(p,Color(0xFF9671BA))}
    roof(175f,85f,126f);roof(430f,85f,126f);roof(302f,35f,142f)
    drawLine(Ink,Offset(302f,34f),Offset(302f,7f),4f);val flag=Path().apply{moveTo(305f,7f);lineTo(346f,18f);lineTo(305f,29f);close()};drawPath(flag,Color(0xFFEFAB9B))
    round(Color(0xFF8763AD),269f,253f,67f,110f,32f);drawLine(Color(0xFFF5D47B),Offset(303f,266f),Offset(303f,356f),3f)
    round(Color(0xFFFFE8A8),164f,196f,25f,43f,12f);round(Color(0xFFFFE8A8),418f,196f,25f,43f,12f)
    star(302f,178f,22f,Color(0xFFF9CC69));star(99f,147f,14f,Color(0xFFF9CC69));star(504f,209f,13f,Color(0xFFF9CC69))
    // A tiny star companion rather than stock emoji.
    star(114f,304f,55f,Color(0xFFFFD77E));eye(101f,301f);eye(126f,301f)
    drawArc(Ink,5f,170f,false,Offset(104f,308f),Size(22f,15f),style=Stroke(3f))
    oval(Color(0xFFAD8EC7),464f,321f,42f,40f);star(484f,314f,13f,Cream)
}
private fun DrawScope.space() {
    val blue=Color(0xFF7FA8E2)
    drawCircle(Color(0xFFC6D9F4),155f,Offset(300f,222f))
    for(i in 0..10)star(55f+(i*67%500),42f+(i*83%320),if(i%3==0)12f else 6f,Color(0xFF638BC6))
    oval(Color(0xFFF7C978),350f,65f,125f,121f)
    drawOval(Color(0xFFE9B16C),Offset(325f,105f),Size(180f,38f),style=Stroke(12f))
    drawCircle(Color(0xFFEFBC6E),15f,Offset(405f,92f));drawCircle(Color(0xFFEFBC6E),10f,Offset(439f,135f))
    // Soft, toy-like rocket and a waving robot navigator.
    val flame=Path().apply{moveTo(247f,315f);quadraticTo(270f,415f,294f,315f);close()};drawPath(flame,Color(0xFFF9CD75))
    val fin=Path().apply{moveTo(233f,230f);lineTo(190f,319f);lineTo(246f,295f);close()};drawPath(fin,Color(0xFFEA9B9C))
    val fin2=Path().apply{moveTo(306f,230f);lineTo(350f,319f);lineTo(295f,295f);close()};drawPath(fin2,Color(0xFFEA9B9C))
    val body=Path().apply{moveTo(270f,93f);cubicTo(206f,152f,213f,244f,236f,313f);lineTo(304f,313f);cubicTo(327f,244f,334f,152f,270f,93f);close()};drawPath(body,Cream)
    val nose=Path().apply{moveTo(270f,93f);quadraticTo(239f,120f,230f,157f);lineTo(310f,157f);quadraticTo(300f,120f,270f,93f);close()};drawPath(nose,Color(0xFFEBA6AA))
    drawCircle(blue,34f,Offset(270f,211f));drawCircle(Color(0xFFCDECF0),24f,Offset(270f,211f));drawCircle(Color.White,6f,Offset(261f,203f))
    round(blue,236f,294f,69f,21f,8f)
    round(Color(0xFF9AC8CA),380f,275f,96f,79f,26f);round(Cream,392f,287f,72f,41f,16f);eye(412f,305f);eye(442f,305f)
    drawLine(Ink,Offset(429f,272f),Offset(429f,250f),4f);drawCircle(Color(0xFFF9CD75),8f,Offset(429f,246f))
    oval(Color(0xFFADC8E7),352f,365f,156f,22f)
}
private fun DrawScope.ocean() {
    val teal=Color(0xFF69B9B6)
    oval(Color(0xFFBDDFD5),53f,351f,497f,45f)
    for(i in 0..6)drawCircle(Color(0xFF8ECBC7),5f+i%3*3,Offset(83f+i*69,77f+(i*47%227)),style=Stroke(3f))
    for(i in 0..3){val x=90f+i*19;val p=Path().apply{moveTo(x,360f);cubicTo(x-35,320f,x+30,295f,x+3,252f)};drawPath(p,Color(0xFF7BB6A1),style=Stroke(14f))}
    round(Color(0xFFF8CC71),176f,159f,262f,162f,80f)
    round(Color(0xFFE5AD5E),187f,290f,240f,28f,14f)
    round(teal,282f,119f,57f,42f,13f);round(teal,307f,101f,63f,20f,9f)
    val tail=Path().apply{moveTo(441f,213f);lineTo(486f,188f);lineTo(486f,286f);lineTo(441f,263f);close()};drawPath(tail,Color(0xFFE5AD5E))
    drawCircle(Color(0xFFDA9F53),53f,Offset(285f,232f));drawCircle(Color(0xFF83CED3),42f,Offset(285f,232f));drawCircle(Color.White.copy(.65f),12f,Offset(271f,217f))
    eye(365f,218f);drawCircle(Color(0xFFE9A094),12f,Offset(391f,245f))
    drawArc(Ink,0f,110f,false,Offset(357f,240f),Size(28f,20f),style=Stroke(3f))
    fun fish(x:Float,y:Float,c:Color){oval(c,x,y,62f,34f);val p=Path().apply{moveTo(x,y+17);lineTo(x-24,y);lineTo(x-24,y+34);close()};drawPath(p,c);drawCircle(Ink,4f,Offset(x+46,y+13))}
    fish(107f,129f,Color(0xFFEC9EA4));fish(422f,80f,teal)
    val shell=Path().apply{moveTo(432f,359f);quadraticTo(380f,293f,430f,301f);quadraticTo(461f,275f,486f,308f);quadraticTo(513f,315f,484f,359f);close()};drawPath(shell,Color(0xFFEBA4AD));drawCircle(Cream,18f,Offset(457f,337f))
    star(176f,357f,20f,Color(0xFFEBA4AD))
}
