"""Deterministic, original low-level game cues. No third-party recordings."""
from pathlib import Path
import numpy as np, wave
out=Path(__file__).resolve().parents[1]/'app/src/main/assets/sound';out.mkdir(exist_ok=True)
sr=24000
rng=np.random.default_rng(17)
def save(name,x):
 x=np.clip(x,-.8,.8);x[:240]*=np.linspace(0,1,240);x[-240:]*=np.linspace(1,0,240)
 with wave.open(str(out/name),'wb') as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(sr);w.writeframes((x*32767).astype('<i2').tobytes())
for k,theme in enumerate(['dino','magic','space','ocean']):
 t=np.arange(sr*8)/sr
 noise=rng.normal(0,1,len(t));soft=np.convolve(noise,np.ones(120)/120,mode='same')
 if theme=='dino':
  x=.18*soft
  for start in [.8,2.5,4.6,6.4]:
   u=t-start;env=np.exp(-((u-.12)/.09)**2);x+=.035*np.sin(2*np.pi*(1600*u+800*u*u))*env
 elif theme=='magic':
  x=np.zeros(len(t))
  for start,f in zip([.2,1.7,3.2,5,6.4],[523.25,659.25,783.99,659.25,1046.5]):
   u=np.maximum(0,t-start);x+=.055*np.sin(2*np.pi*f*u)*np.exp(-u*2)*(t>=start)
 elif theme=='space':x=.045*np.sin(2*np.pi*110*t)+.025*np.sin(2*np.pi*165*t)+.06*soft
 else:
  x=.14*soft
  for start in [.3,1.2,2.6,4.1,5.8,7.1]:
   u=t-start;env=np.exp(-((u-.06)/.035)**2);x+=.055*np.sin(2*np.pi*(420*u+1400*u*u))*env
 save(theme+'_ambience.wav',x)
 t=np.arange(int(sr*1.15))/sr
 x=np.zeros(len(t))
 for j,f in enumerate([[220,330,440],[523.25,659.25,783.99],[164.81,246.94,329.63],[392,523.25,659.25]][k]):
  u=np.maximum(0,t-j*.18);x+=.11*np.sin(2*np.pi*f*u)*np.exp(-u*5)*(t>=j*.18)
 save(theme+'_departure.wav',x)
print('Created eight original offline sound assets.')
