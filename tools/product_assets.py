"""Original modeled assets informed by the manufacturer references in docs/product-references.md.
No manufacturer photo or logo is embedded in these textures.
"""
import json
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
A=Path(__file__).resolve().parents[1]/'src/main/resources/assets/homesystems'
try:
 font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',11);small=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',8)
except OSError:
 font=small=ImageFont.load_default()
D={
'lobby_phone':(2.6,1.5,15.159,13.4,13.45,16,'#a9aeb2','#15191d'),
'video_phone':(.5,3.5,15.6,15.5,12.7,16,'#bfc3c4','#283c64'),
'interphone':(6.1,1,14.4,9.9,14.3,16,'#e7e7e1','#ddddda'),
'wallpad':(.5,3,15.375,15.5,13.45,16,'#42464a','#101419'),
'guard_station':(1.5,1.2,10,14.5,13.5,11.15,'#595c61','#25282e')}
def write(p,x):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(x,ensure_ascii=False,indent=2)+'\n')
def text(d,xy,t,size=font,fill='#e0e6ea'):d.text(xy,t,font=size,fill=fill,anchor='mm')
class HighResDraw:
 def __init__(self,image):self.draw=ImageDraw.Draw(image)
 def __getattr__(self,name):
  def draw(coords,*args,**kwargs):
   def scaled(v):return tuple(scaled(x) for x in v) if isinstance(v,(tuple,list)) else v*4
   if 'font' in kwargs:kwargs['font']=kwargs['font'].font_variant(size=kwargs['font'].size*4)
   for key in ['radius','width']:
    if key in kwargs:kwargs[key]*=4
   if name in ['rectangle','rounded_rectangle','ellipse','line']:kwargs.setdefault('width',4)
   return getattr(self.draw,name)(scaled(coords),*args,**kwargs)
  return draw
def front(name,off):
 *_,body,face=D[name];im=Image.new('RGB',(1024,1024),body);d=HighResDraw(im)
 for i in range(6):d.rounded_rectangle((i,i,255-i,255-i),radius=max(2,12-i),outline=['#33373b','#8e969c','#e6e8e9','#b1b7ba','#737a7e','#4b5054'][i])
 d.rounded_rectangle((10,9,245,237),radius=4,fill=face)
 if name=='lobby_phone':
  d.rectangle((25,39,104,160),fill='#080e17',outline='#3b4655');d.ellipse((165,32,214,81),fill='#d1d4d7');d.ellipse((173,40,206,73),fill='#0a1118',outline='#59626a');d.ellipse((184,50,196,62),fill='#1e3554');d.point((186,51),fill='#bbe5fc')
  for row in range(4):
   for col in range(3):text(d,(163+col*24,99+row*24),str(row*3+col+1) if row<3 else ['X','0','#'][col])
  for x in range(20,238,3):d.line((x,229,x,245),fill='#7b8186')
  d.ellipse((183,188,215,218),outline='#ccd4d9',width=2);text(d,(199,203),'CALL',small)
 elif name=='video_phone':
  d.rectangle((62,12,235,224),fill=face);d.rectangle((74,43,229,178),fill='#0b1119',outline='#838e98')
  for x,label in [(98,'CAM'),(139,'GUARD'),(180,'DOOR'),(221,'MENU')]:d.ellipse((x-10,192,x+10,216),outline='#aab3bd');text(d,(x,204),label,small)
 elif name=='wallpad':
  d.rectangle((14,16,241,224),fill='#0b1119',outline='#6a7278');d.line((15,17,239,17),fill='#a6abb0');d.rectangle((9,238,246,249),fill='#72787d')
 elif name=='guard_station':
  d.rectangle((72,15,240,132),fill='#090e17',outline='#7f878f')
  for row in range(4):
   for col in range(3):
    x,y=86+col*24,151+row*20;d.ellipse((x-8,y-8,x+8,y+8),fill='#171b20',outline='#69727b');text(d,(x,y),str(row*3+col+1) if row<3 else ['*','0','#'][col],small)
  d.ellipse((171,166,221,216),fill='#9b9ea2',outline='#dfe1e4',width=2);d.ellipse((183,178,209,204),fill='#555a61');text(d,(196,157),'OPEN',small)
 else:
  d.rectangle((60,15,196,235),fill='#b6b8b4',outline='#f4f4ef')
  d.rounded_rectangle((85,188,171,208),radius=7,fill='#e8e8e3',outline='#868b89');text(d,(128,198),'GUARD',small,fill='#636a67')
  d.rounded_rectangle((85,214,171,234),radius=7,fill='#e8e8e3',outline='#868b89');text(d,(128,224),'DOOR',small,fill='#636a67')
 if name in ['video_phone','guard_station','interphone']:
  if name=='interphone':x0,x1,y0,y1=55,201,12,185
  else:x0,x1,y0,y1=12,55,10,235
  # Dark cradle remains visible on the base texture; the handset is a separate physical mesh.
  d.rounded_rectangle((x0,y0,x1,y1),radius=8,fill='#747b7c' if name=='interphone' else '#171b20')
  if off:dx,dy=-8,-10
  else:dx,dy=0,0
  hd=HighResDraw(im);fill='#e8e9e5' if name=='interphone' else '#c7cace' if name=='video_phone' else '#3e4148'
  # GUI handset preview; the block base uses a separate cradle-only image below.
  if off is not None:
   hd.rounded_rectangle((x0+dx,y0+dy,x1+dx,y1+dy),radius=12,fill=fill,outline='#8d9498',width=2)
   for yy in [y0+16,y1-24]:
    for xx in range(int(x0+8+dx),int(x1-5+dx),4):hd.ellipse((xx,yy+dy,xx+1,yy+dy+1),fill='#62696c')

 return im.resize((512,512),Image.Resampling.LANCZOS)
for name,v in D.items():
 x0,y0,z0,x1,y1,z1,body,face=v
 for off,label in [(False,''),(True,'_offhook'),(None,'_base')]:front(name,off).save(A/f'textures/block/{name}{label}.png')
 # A small independently replaceable PNG for logos, used by UI and block renderer.
 logo=A/f'textures/logo/{name}.png';logo.parent.mkdir(parents=True,exist_ok=True)
 im=Image.new('RGBA',(128,16));text(ImageDraw.Draw(im),(64,8),'HOME SYSTEMS',small);im.save(logo)
 for off in [False,True]:
  faces={f:{'texture':'#body','uv':[0,0,16,16]} for f in ['up','down','east','west','south']};faces['north']={'texture':'#front','uv':[0,0,16,16]}
  element={'from':[x0,y0,z0],'to':[x1,y1,z1],'faces':faces}
  rotate={'origin':[8,1.2,10],'axis':'x','angle':22.5}
  if name=='guard_station':element['rotation']=rotate
  elements=[element]
  if name=='guard_station':elements.append({'from':[1.5,0,3],'to':[14.5,1.2,15],'faces':{f:{'texture':'#body'} for f in faces}})
  if name in ['video_phone','guard_station','interphone']:
   # Three boxes describe a solid receiver without any cord.
   hx0,hx1=(x1-(x1-x0)*.22,x1-.25) if name!='interphone' else (x0+.15,x1-.15)
   hy0,hy1=y0+.5,y1-.5 if name!='interphone' else y1-1
   dz=-1 if off else 0;dx=1.3 if off else 0;dy=1 if off else 0
   for low,high,width in [(hy0,hy0+1.7,0),(hy0+1.7,hy1-1.7,.2),(hy1-1.7,hy1,0)]:
    e={'from':[hx0+width+dx,low+dy,z0-1+dz],'to':[hx1-width+dx,high+dy,z0+.03+dz],'faces':{f:{'texture':'#receiver','uv':[0,0,16,16]} for f in faces}}
    if name=='guard_station':e['rotation']=rotate
    elements.append(e)
   rec=Image.new('RGB',(16,16),'#e8e9e5' if name=='interphone' else '#c7cace' if name=='video_phone' else '#3e4148');rd=ImageDraw.Draw(rec);rd.rectangle((0,0,15,15),outline='#7e8589')
   for yy in [3,5,10,12]:
    for xx in [4,7,10]:rd.point((xx,yy),fill='#4a5359')
   rec.save(A/f'textures/block/{name}_receiver.png')
  model={'ambientocclusion':False,'textures':{'front':f'homesystems:block/{name}_base','body':f'homesystems:block/{name}_side','receiver':f'homesystems:block/{name}_receiver','particle':f'homesystems:block/{name}_base'},'elements':elements,'display':{'gui':{'rotation':[15,160,0],'scale':[.9,.9,.9]},'fixed':{'rotation':[0,180,0]},'firstperson_righthand':{'rotation':[0,135,0],'scale':[.6,.6,.6]},'thirdperson_righthand':{'rotation':[75,45,0],'scale':[.5,.5,.5]}}}
  write(A/f'models/block/{name}{"_offhook" if off else ""}.json',model)
 side=Image.new('RGB',(16,16),body);sd=ImageDraw.Draw(side);sd.rectangle((0,0,15,15),outline='#686f75');side.save(A/f'textures/block/{name}_side.png')
 variants={}
 for facing,angle in [('north',0),('east',90),('south',180),('west',270)]:
  for off in [False,True]:variants[f'facing={facing},offhook={str(off).lower()}']={'model':f'homesystems:block/{name}{"_offhook" if off else ""}','y':angle}
 write(A/f'blockstates/{name}.json',{'variants':variants})
print('Generated distinct reference-informed panels and cord-free receiver states for five products.')

# Entrance devices retain their existing design and gain full-sized GUI fronts and replaceable logos.
for name in ['door_camera','door_phone']:
 p=A/f'textures/block/{name}.png';im=Image.open(p);im.resize((256,256),Image.Resampling.NEAREST).save(p)
 logo=Image.new('RGBA',(128,16));text(ImageDraw.Draw(logo),(64,8),'HOME SYSTEMS',small);logo.save(A/f'textures/logo/{name}.png')
Image.new('RGB',(1,1),'white').save(A/'textures/block/panel_white.png')
