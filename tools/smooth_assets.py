"""Continuous client meshes replace all segmented block and inventory models.
Preserve product layouts, generate antialiased receiver/entrance artwork and materials.
"""
import json, math
from pathlib import Path
from PIL import Image,ImageDraw
A=Path(__file__).resolve().parents[1]/'src/main/resources/assets/homesystems'
colors={'lobby_phone':'#a9aeb2','video_phone':'#bfc3c4','interphone':'#e7e7e1','wallpad':'#42464a','guard_station':'#595c61','door_camera':'#a7b5be','door_phone':'#d9d4c7','sht_308':'#e6e2cf'}
for name,color in colors.items():
 Image.new('RGB',(16,16),color).save(A/f'textures/block/{name}_side.png')
 for suffix in ['', '_offhook']:
  path=A/f'models/block/{name}{suffix}.json'
  if not path.exists():continue
  data=json.loads(path.read_text());data['elements']=[];data['ambientocclusion']=False
  path.write_text(json.dumps(data,indent=2)+'\n')
 path=A/f'models/item/{name}.json'
 old=json.loads(path.read_text()) if path.exists() else {}
 item={'parent':'builtin/entity','display':old.get('display',{'gui':{'rotation':[15,160,0],'scale':[.9,.9,.9]},'fixed':{'rotation':[0,180,0]},'firstperson_righthand':{'rotation':[0,135,0],'scale':[.6,.6,.6]},'thirdperson_righthand':{'rotation':[75,45,0],'scale':[.5,.5,.5]}})}
 path.write_text(json.dumps(item,indent=2)+'\n')
 if name not in ['sht_308','door_camera','door_phone']:
  # Base, on-hook and off-hook panels were supersampled by product_assets.py.
  pass
 if name in ['interphone','video_phone','guard_station']:
  body={'interphone':(232,233,229),'video_phone':(199,202,206),'guard_station':(62,65,72)}[name]
  im=Image.new('RGB',(256,1024));d=ImageDraw.Draw(im)
  for x in range(256):
   u=(x-128)/128;gain=1+.055*math.exp(-((u+.5)/.18)**2)-.12*abs(u)**8
   d.line((x,0,x,1023),fill=tuple(min(255,round(c*gain)) for c in body))
  for cy in [74,950]:
   for dx,dy in [(-24,-16),(0,-20),(24,-16),(-24,16),(0,20),(24,16)]:
    d.ellipse((128+dx-2,cy+dy-2,128+dx+2,cy+dy+2),fill='#50585c')
  im.resize((128,512),Image.Resampling.LANCZOS).save(A/f'textures/block/{name}_receiver_smooth.png')
# Replace the nearest-neighbour enlarged 32px entrance panels with vector-drawn artwork.
for name in ['door_camera','door_phone']:
 S=3;im=Image.new('RGB',(512*S,512*S),colors[name]);d=ImageDraw.Draw(im)
 def box(b,fill,r=0,outline=None,width=1):d.rounded_rectangle(tuple(round(v*S) for v in b),round(r*S),fill,outline,round(width*S))
 def oval(b,fill,outline=None,width=1):d.ellipse(tuple(round(v*S) for v in b),fill,outline,round(width*S))
 box((6,6,506,506),colors[name],22,'#5e686b',2)
 if name=='door_camera':
  box((105,63,407,239),'#24343f',12,'#718993',2)
  oval((181,72,331,222),'#c0d0da');oval((190,81,322,213),'#08111b');oval((225,116,287,178),'#244958');oval((235,121,253,139),'#a1d3dd')
  for y in [280,296]:box((124,y,388,y+5),'#485d68',2)
 else:
  box((105,63,407,275),'#424a49',12,'#86938c',2)
  for y in range(85,250,24):box((135,y,377,y+7),'#202e32',3)
 box((155,369,357,448),'#66d7c1' if name=='door_camera' else '#e7bd69',21,'#53645f',3)
 result=im.resize((512,512),Image.Resampling.LANCZOS)
 for suffix in ['', '_base']:result.save(A/f'textures/block/{name}{suffix}.png')
print('Smooth surfaces: all eight products; shared world/item meshes, opaque materials, antialiased panels.')
