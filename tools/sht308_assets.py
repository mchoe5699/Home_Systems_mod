"""SHT-308 original model/texture drawn from the user's three photographs and manual p6.
The telephone cord is deliberately omitted; no user photograph/PDF is embedded in the JAR.
"""
import json,math,os
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
A=Path(__file__).resolve().parents[1]/'src/main/resources/assets/homesystems';DATA=A.parents[1]/'data/homesystems'
def write(p,x):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(x,ensure_ascii=False,indent=2)+'\n')
latin=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',12)
bold=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf',24)
krpath=next((p for p in [os.environ.get('SHT_FONT',''),'/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc','/workspace/toolchains/NotoSansKR.ttf'] if p and Path(p).exists()),None)
if krpath is None:raise RuntimeError('Set SHT_FONT to a Korean-capable font (Noto Sans CJK / Noto Sans KR).')
kfont=ImageFont.truetype(krpath,11)
cream='#e5e1c9';edge='#bbb8a4';shadow='#9c9b8e';ink='#454749'
def rounded_gradient(im,box,radius,color):
 x0,y0,x1,y1=map(int,box);mask=Image.new('L',im.size);ImageDraw.Draw(mask).rounded_rectangle(box,radius=radius,fill=255)
 layer=Image.new('RGBA',im.size);d=ImageDraw.Draw(layer);base=tuple(int(color[i:i+2],16) for i in [1,3,5])
 for x in range(x0,x1+1):
  u=(x-x0)/max(1,x1-x0);gain=1+.08*math.exp(-((u-.11)/.08)**2)-.16*math.exp(-((u-.99)/.035)**2)
  rgb=tuple(min(255,int(c*gain)) for c in base);d.line((x,y0,x,y1),fill=rgb+(255,))
 im.paste(layer,(0,0),mask);ImageDraw.Draw(im).rounded_rectangle(box,radius=radius,outline=edge,width=2)
def base():
 im=Image.new('RGBA',(512,512));d=ImageDraw.Draw(im)
 rounded_gradient(im,(22,23,152,488),15,'#cccbb8')
 d.rounded_rectangle((30,30,132,165),radius=12,fill='#b2b09e',outline='#f4efda',width=3)
 d.rounded_rectangle((34,353,139,474),radius=13,fill='#d8d4bf',outline='#fbf5de',width=3)
 d.rectangle((66,45,103,70),fill='#e6e2d0',outline='#9b9b8a')
 d.rounded_rectangle((37,176,139,295),radius=8,fill='#dad7c3',outline='#aaa998',width=2)
 for y in range(191,281,9):
  span=43*math.sqrt(max(0,1-((y-236)/49)**2));d.line((89-span,y,89+span,y),fill='#646b68',width=3)
 rounded_gradient(im,(150,5,507,505),19,cream)
 d.line((442,222,444,490),fill='#ccc8b2',width=2)
 # Recessed CRT, separate black speaker bay, beveled glass, no color UI or fake camera.
 d.rounded_rectangle((169,23,491,221),radius=10,fill='#25282a',outline='#9d9f92',width=3)
 d.rounded_rectangle((176,29,481,212),radius=7,fill='#101518')
 d.rectangle((190,43,400,210),fill='#151922');d.rectangle((201,57,385,201),fill='#373540',outline='#2b2b34',width=5)
 d.polygon([(205,59),(383,59),(376,71),(209,71)],fill='#45414a')
 d.line((405,33,405,212),fill='#464a46',width=2)
 for y in range(81,192,9):d.line((423,y,474,y),fill='#303736',width=3)
 d.text((447,52),'전원',font=kfont,fill='#777c74',anchor='mm');d.ellipse((446,64,451,69),fill='#3f5042')
 # Four vertically arranged physical buttons, with printed Korean legends.
 for cy,label in [(286,'비 상'),(330,'경비실'),(374,'외 출'),(431,'현 관')]:
  d.text((467,cy-24),label,font=kfont,fill=ink,anchor='mm')
  radius=14 if cy!=431 else 18
  d.ellipse((467-radius-2,cy-radius-2,467+radius+2,cy+radius+2),fill='#a5a795',outline='#f3edd5',width=2)
  d.ellipse((467-radius,cy-radius,467+radius,cy+radius),fill='#666d77' if cy!=431 else '#e3e0ca',outline='#8d928a',width=2)
  d.arc((467-radius+2,cy-radius+2,467+radius-2,cy+radius-2),190,340,fill='#dddac7',width=2)
 # HOME-TECH / model name printed at the photographed lower-left corner.
 d.line((176,478,188,468,201,478),fill='#555e5a',width=3);d.rectangle((180,478,197,488),outline='#555e5a',width=2)
 d.text((207,475),'SHT-308',font=latin,fill=ink);d.text((207,488),'VIDEO DOOR PHONE',font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',7),fill=ink)
 # Real side/bottom adjustment switches are intentionally small; hovering gives their function.
 d.rectangle((135,391,146,447),fill='#aaa994')
 for xx in [283,384]:d.rectangle((xx,498,xx+46,505),fill='#9c9e8e')
 d.rectangle((497,116,506,186),fill='#aaa997')
 return im
receiver=Image.new('RGBA',(128,512));rounded_gradient(receiver,(3,2,124,507),37,'#eee9d6');outer=receiver.copy();outer.save(A/'textures/block/sht_308_receiver_outer.png');rd=ImageDraw.Draw(receiver)
for cy in [36,474]:
 for dx in [-16,0,16]:
  for dy in [-8,0,8]:rd.ellipse((64+dx-1,cy+dy-1,64+dx+1,cy+dy+1),fill='#ccc8b8')
receiver.save(A/'textures/block/sht_308_receiver.png')
b=base();b.save(A/'textures/block/sht_308_base.png');b.save(A/'textures/block/sht_308_offhook.png')
normal=b.copy();normal.alpha_composite(outer.resize((112,506)),(4,1));normal.save(A/'textures/block/sht_308.png')
side=Image.new('RGB',(64,64),cream);ImageDraw.Draw(side).rectangle((0,0,63,63),outline=edge,width=3);side.save(A/'textures/block/sht_308_side.png')
logo=Image.new('RGBA',(256,40));ld=ImageDraw.Draw(logo);font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf',28);xx=1
for letter in 'SAMSUNG':ld.text((xx,0),letter,font=font,fill='#292e30');xx+=ld.textlength(letter,font=font)+2
(A/'textures/logo').mkdir(parents=True,exist_ok=True);logo.save(A/'textures/logo/sht_308.png')
def cube(a,b,uv=(0,0,16,16),texture='#body'):
 faces={f:{'texture':texture,'uv':list(uv)} for f in ['north','south','west','east','up','down']};return {'from':a,'to':b,'faces':faces}
for off in [False,True]:
 body=cube([1,1.2,13.3],[10.9,14.4,16]);body['faces']['north']={'texture':'#front','uv':[76/16,2/16,256/16,252/16]}
 cradle=cube([10.6,1.4,12.6],[14.8,14.2,15.5]);cradle['faces']['north']={'texture':'#front','uv':[4/16,6/16,79/16,250/16]}
 elements=[body,cradle]
 # Raised bezel around the curved CRT glass, narrow speaker bay, separate stepped button caps.
 # Each front cap sits where its printed legend is located on the same normalized panel.
 for yy,rad in [(286,.38),(330,.38),(374,.38),(431,.49)]:
  cx=15-467/512*14;cy=14.5-yy/512*13.5
  for band in range(5):
   by0=cy-rad+2*rad*band/5;by1=cy-rad+2*rad*(band+1)/5;half=rad*math.sqrt(max(.1,1-((band-2)/2.6)**2))
   cap=cube([cx-half,by0,13.02],[cx+half,by1,13.31]);cap['faces']['north']={'texture':'#front','uv':[(467-half/14*512)/32,(yy-(by1-cy)/13.5*512)/32,(467+half/14*512)/32,(yy-(by0-cy)/13.5*512)/32]};elements.append(cap)
 dx,dy,dz=(4.2,1,-1.8) if off else (0,0,0)
 for i in range(18):
  y0=1.05+i*13.4/18;y1=1.05+(i+1)*13.4/18;t=(i+.5)/18
  half=1.42*(.85+.15*abs(2*t-1));half*=min(1,math.sqrt(max(.1,1-((abs(t-.5)-.41)/.13)**2)) if abs(t-.5)>.41 else 1)
  c=cube([13.45-half+dx,y0+dy,10.65+dz],[13.45+half+dx,y1+dy,12.85+dz],texture='#outer')
  c['faces']['north']['texture']='#receiver' if off else '#outer'
  c['faces']['north']['uv']=[0,16*(1-(i+1)/18),16,16*(1-i/18)];elements.append(c)
 model={'ambientocclusion':True,'textures':{'front':'homesystems:block/sht_308_base','body':'homesystems:block/sht_308_side','receiver':'homesystems:block/sht_308_receiver','outer':'homesystems:block/sht_308_receiver_outer','particle':'homesystems:block/sht_308_side'},'elements':elements,'display':{'gui':{'rotation':[12,165,0],'scale':[.85,.85,.85]},'fixed':{'rotation':[0,180,0]},'firstperson_righthand':{'rotation':[0,135,0],'scale':[.6,.6,.6]},'thirdperson_righthand':{'rotation':[75,45,0],'scale':[.5,.5,.5]}}}
 write(A/f'models/block/sht_308{"_offhook" if off else ""}.json',model)
write(A/'models/item/sht_308.json',{'parent':'homesystems:block/sht_308'})
write(A/'blockstates/sht_308.json',{'variants':{f'facing={face},offhook={str(off).lower()}':{'model':'homesystems:block/sht_308'+('_offhook' if off else ''),'y':angle} for face,angle in [('north',0),('east',90),('south',180),('west',270)] for off in [False,True]}})
write(DATA/'recipes/sht_308.json',{'type':'minecraft:crafting_shaped','category':'redstone','pattern':['III','RGR','ICI'],'key':{'I':{'item':'minecraft:iron_ingot'},'R':{'item':'minecraft:redstone'},'G':{'item':'minecraft:glass_pane'},'C':{'item':'minecraft:quartz'}},'result':{'item':'homesystems:sht_308'}})
write(DATA/'loot_tables/blocks/sht_308.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'homesystems:sht_308'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
print('Generated SHT-308: cream CRT panel, cradle, four physical buttons and cord-free receiver states.')

# A translucent circular lamp overlays each of the first three physical button caps.
lamp=Image.new('RGBA',(32,32));ImageDraw.Draw(lamp).ellipse((2,2,29,29),fill='white');lamp.save(A/'textures/block/panel_lamp.png')
