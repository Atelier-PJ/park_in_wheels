"""Render four one-shot, offline vehicle transformations from matching artwork."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter
import math
root=Path(__file__).resolve().parents[1]
before=Image.open(root/'docs/artwork/vehicles/before.png').convert('RGB')
after=Image.open(root/'docs/artwork/vehicles/after.png').convert('RGB')
out=root/'app/src/main/assets/vehicles';out.mkdir(exist_ok=True)
boxes=[(19,16,751,498),(785,16,1517,498),(19,526,751,1004),(785,526,1517,1004)]
for theme,box in zip(['dino','magic','space','ocean'],boxes):
 a=before.crop(box).resize((732,480),Image.Resampling.LANCZOS)
 b=after.crop(box).resize((732,480),Image.Resampling.LANCZOS)
 b.save(out/f'{theme}_ready.webp',quality=94)
 frames=[]
 for i in range(60):
  t=i/59
  progress=max(0,min(1,(t-.1)/.64))
  # A feathered sweep introduces roof equipment first, then body costume.
  edge=-100+progress*730
  mask=Image.new('L',a.size,0);d=ImageDraw.Draw(mask)
  if edge>0:d.rectangle((0,0,732,int(edge)),fill=255)
  mask=mask.filter(ImageFilter.GaussianBlur(26))
  frame=Image.composite(b,a,mask).convert('RGBA') if progress<1 else b.convert('RGBA')
  if .1<t<.79:
   fx=Image.new('RGBA',a.size);d=ImageDraw.Draw(fx)
   strength=math.sin(math.pi*progress)
   for k in range(12):
    x=80+(k*137)%570;y=edge+25*math.sin(k*1.7+t*12)
    radius=3+4*(.5+.5*math.sin(k+t*16));alpha=int(135*strength)
    d.polygon([(x,y-radius*2),(x+radius*.45,y-radius*.45),(x+radius*2,y),(x+radius*.45,y+radius*.45),(x,y+radius*2),(x-radius*.45,y+radius*.45),(x-radius*2,y),(x-radius*.45,y-radius*.45)],fill=(255,242,190,alpha))
   frame=Image.alpha_composite(frame,fx)
  frames.append(frame.convert('RGB'))
 # Global palette prevents distracting palette shimmer between GIF frames.
 palette=b.quantize(colors=256,method=Image.Quantize.MEDIANCUT)
 encoded=[f.quantize(palette=palette,dither=Image.Dither.NONE) for f in frames]
 encoded[0].save(out/f'{theme}_transform.gif',save_all=True,append_images=encoded[1:],duration=[70]*60,disposal=1,optimize=False)
 with Image.open(out/f'{theme}_transform.gif') as gif:
  duration=sum((gif.seek(j) or gif.info.get('duration',0)) for j in range(gif.n_frames))
  assert duration==4200 and 'loop' not in gif.info
  print(theme,gif.n_frames,duration,(out/f'{theme}_transform.gif').stat().st_size)
