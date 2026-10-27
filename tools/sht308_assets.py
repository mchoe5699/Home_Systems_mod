"""Rebuilt SHT-308, measured against the supplied front/side/off-hook photos.
Drawn artwork only: references are not redistributed. No telephone cord.
Coordinates below are shared between the textured mesh and interactive panel.
"""
import json, math, os
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

A = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/homesystems'
DATA = A.parents[1] / 'data/homesystems'
T = A / 'textures/block'
S = 3  # antialias source artwork; Minecraft receives ordinary power-of-two PNGs
CREAM = (230, 226, 207)
FONT = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
KR = next((p for p in [os.environ.get('SHT_FONT', ''), '/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc', '/workspace/toolchains/NotoSansKR.ttf'] if p and Path(p).exists()), None)
if not KR:
    raise RuntimeError('Set SHT_FONT to a Korean-capable Noto font.')

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')

def scaled(box):
    return tuple(round(v*S) for v in box)

class Art:
    def __init__(self, width=512, height=512):
        self.im = Image.new('RGBA', (width*S, height*S))
        self.d = ImageDraw.Draw(self.im)
    def rect(self, box, fill, radius=0, outline=None, width=1):
        self.d.rounded_rectangle(scaled(box), round(radius*S), fill=fill, outline=outline, width=round(width*S))
    def line(self, points, fill, width=1):
        self.d.line(scaled(points), fill=fill, width=round(width*S))
    def oval(self, box, fill, outline=None, width=1):
        self.d.ellipse(scaled(box), fill=fill, outline=outline, width=round(width*S))
    def text(self, at, value, size, fill, font=FONT, anchor=None):
        self.d.text(scaled(at), value, font=ImageFont.truetype(font, round(size*S)), fill=fill, anchor=anchor)
    def shell(self, box, radius, rgb=CREAM):
        mask=Image.new('L', self.im.size)
        ImageDraw.Draw(mask).rounded_rectangle(scaled(box), round(radius*S), fill=255)
        layer=Image.new('RGBA', self.im.size)
        draw=ImageDraw.Draw(layer)
        x0,y0,x1,y1=scaled(box)
        for x in range(x0,x1+1):
            u=(x-x0)/max(1,x1-x0)
            gain=1 + .065*math.exp(-((u-.07)/.065)**2) - .14*math.exp(-((u-.998)/.035)**2)
            draw.line((x,y0,x,y1), fill=tuple(min(255,round(c*gain)) for c in rgb)+(255,))
        self.im.paste(layer,(0,0),mask)
        self.rect(box,None,radius, '#c3c0b1',.8)
    def save(self, name):
        self.im.resize((self.im.width//S,self.im.height//S),Image.Resampling.LANCZOS).save(T/name)

# Exposed cradle: upper hook, horizontal grille, lower dished receiver seat.
a=Art()
a.shell((20,24,157,492),17,(211,209,192))
a.rect((29,34,140,162),'#b9b6a5',15, '#f6f0dd', 2)
a.rect((47,45,112,112),'#d9d7c5',6)
a.rect((66,48,103,91),'#a4a496',2)
a.rect((69,49,102,86),'#ece8d7',1)
a.shell((31,177,143,318),12,(224,220,201))
for y in range(187,307,9):
    span=47*math.sqrt(max(0,1-((y-248)/65)**2))
    a.rect((87-span,y,87+span,y+2.7),'#454b42',1)
    a.line((87-span,y+3,87+span,y+3),'#f4efdb',.6)
a.rect((31,354,143,480),'#f3efdf',15)
a.rect((36,361,139,477),'#b6b4a4',11)
a.rect((40,366,139,474),'#dcd9c8',10)
# Main shell: curved outer edges, narrow rounded control strip, empty cream face.
a.shell((151,5,507,505),20)
for x in range(417,455):
    t=(x-417)/38
    shade=round(224+9*math.sin(t*math.pi))
    a.line((x,224,x,484),(shade,shade-4,shade-21),.5)
a.line((443,223,446,489),'#d1ccba',.8)
# Raised black surround, recessed CRT and separate slatted speaker.
a.rect((169,23,491,221),'#c8c7b7',10)
a.rect((172,25,489,218),'#10171a',8,'#343b3c',1.5)
a.rect((179,32,406,212),'#202427',4)
a.rect((189,42,399,209),'#11171f',4)
a.rect((200,56,386,202),'#34333d',3)
a.line((204,60,380,60),'#4e4c55',3)
a.line((204,64,379,64),'#3f3e48',2)
a.line((203,197,382,197),'#262730',2)
a.line((405,32,405,213),'#444747',1)
a.rect((411,34,482,210),'#10191a',3)
for y in range(81,199,7):
    a.line((421,y,475,y),'#2c3634',1.4)
    a.line((423,y+1.5,473,y+1.5),'#080f10',.8)
a.text((449,51),'전원',8,'#788178',KR,'mm')
a.oval((446,63,451,68),'#24382b', '#131e18',.6)
# The first three caps are blue-grey; the larger entrance cap is ivory.
for cy,label in [(286,'비  상'),(330,'경비실'),(374,'외  출'),(431,'현  관')]:
    a.text((467,cy-24),label,10,'#55584f',KR,'mm')
    r=14 if cy!=431 else 18
    a.oval((467-r-2,cy-r-2,467+r+2,cy+r+2),'#989d94')
    a.oval((467-r-1,cy-r-1,467+r+1,cy+r+1),'#eeead9')
    a.oval((467-r,cy-r,467+r,cy+r),'#596673' if cy!=431 else '#d9d9c9')
    a.oval((467-r+2,cy-r+1,467+r-2,cy+r-3),'#65727f' if cy!=431 else '#e8e7d7')
    a.line((467-r+5,cy-r+4,467+r-6,cy-r+4),'#a3abb0' if cy!=431 else '#faf8eb',1)
# Original small home emblem and model print, including the lower-left placement.
a.line((176,478,188,468,201,478),'#4d5858',2)
a.line((180,476,180,487,185,487,185,480,191,480,191,487,197,487,197,476),'#4d5858',1.5)
a.text((207,472),'SHT-308',8,'#4d5552')
a.text((207,482),'VIDEO DOOR PHONE',5,'#4d5552')
# Side and underside switches have textured recesses and moving render overlays.
a.rect((135,391,146,447),'#a8aa9a',2)
a.rect((138,395,143,443),'#777e71',1)
for xx in [283,384]: a.rect((xx,498,xx+46,505),'#8e9685',1)
a.rect((497,116,506,186),'#959e8d',2)
a.save('sht_308_base.png');a.save('sht_308_offhook.png')
# GUI artwork needs transparent silhouettes. Mesh faces must be opaque: their
# geometry already defines the silhouette, and cut-out pixels expose internal
# shoulders as false saw teeth at oblique angles.
def opaque_mesh(source, name):
    material=Image.new('RGBA',source.size,CREAM+(255,))
    material.alpha_composite(source)
    material.convert('RGB').save(T/name)
opaque_mesh(a.im.resize((512,512),Image.Resampling.LANCZOS),'sht_308_mesh.png')

# Receiver silhouette varies along its length, with rounded earpiece/mouthpiece ends.
def receiver_half(t):
    # Narrow handle centre, widened ends, smoothly rounded tips.
    width=46 + 9*(abs(2*t-1)**.65)
    if t<.07: width*=math.sqrt(max(0,1-((.07-t)/.07)**2))
    if t>.93: width*=math.sqrt(max(0,1-((t-.93)/.07)**2))
    return width

def receiver(inner):
    r=Art(128,512)
    for row in range(3*S,508*S):
        t=(row/S-3)/505
        half=receiver_half(t)
        for col in range(max(0,round((64-half)*S)),min(128*S,round((64+half)*S))):
            u=(col/S-64)/max(.1,half)
            gain=1+.055*math.exp(-((u+.55)/.18)**2)-.13*abs(u)**9
            rgb=tuple(min(255,round(c*gain)) for c in (238,234,217))
            r.d.point((col,row),fill=rgb+(255,))
    if inner:
        for cy in [48,463]:
            r.oval((30,cy-29,98,cy+29),'#dfdece')
            for dx,dy in [(0,0),(-13,-10),(0,-13),(13,-10),(-13,9),(0,13),(13,9)]:
                r.oval((63+dx-1.2,cy+dy-1.2,63+dx+1.2,cy+dy+1.2),'#717e77')
        r.oval((58,183,70,195),'#c6c7ba','#edeada',.8)
        r.oval((58,320,70,332),'#c6c7ba','#edeada',.8)
        r.rect((14,93,114,112),'#c2c4b6',3)
        r.rect((14,400,114,418),'#c2c4b6',3)
    r.save('sht_308_receiver.png' if inner else 'sht_308_receiver_outer.png')
    opaque_mesh(r.im.resize((128,512),Image.Resampling.LANCZOS),'sht_308_receiver_mesh.png' if inner else 'sht_308_receiver_outer_mesh.png')
    return r.im.resize((128,512),Image.Resampling.LANCZOS)
outer=receiver(False);receiver(True)
normal=a.im.resize((512,512),Image.Resampling.LANCZOS)
normal.alpha_composite(outer.resize((116,506),Image.Resampling.LANCZOS),(3,1))
normal.save(T/'sht_308.png')
# Uniform material on the narrow curved mesh shoulders: outlining each tiny
# section would create artificial horizontal seams on the physical receiver.
Image.new('RGB',(64,64),CREAM).save(T/'sht_308_side.png')
logo=Image.new('RGBA',(512,80));ld=ImageDraw.Draw(logo)
f=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf',51)
x=1
for letter in 'SAMSUNG':
    ld.text((x,0),letter,font=f,fill='#353c3b');x+=ld.textlength(letter,font=f)+4
(A/'textures/logo').mkdir(parents=True,exist_ok=True)
logo.resize((256,40),Image.Resampling.LANCZOS).save(A/'textures/logo/sht_308.png')

# Mesh coordinates map the entire photographed face to u/v=0..512. Every raised
# component uses that same mapping; the world panel and GUI have identical layout.
def X(u): return 15-u*14/512
def Y(v): return 14.5-v*13.5/512

def cuboid(x0,y0,z0,x1,y1,z1,texture='#body',front_uv=None):
    faces={f:{'texture':texture,'uv':[0,0,16,16]} for f in ['north','south','east','west','up','down']}
    if front_uv is not None: faces['north']={'texture':'#front','uv':[v/32 for v in front_uv]}
    return {'from':[round(x0,5),round(y0,5),round(z0,5)],'to':[round(x1,5),round(y1,5),round(z1,5)],'faces':faces}

def panel(elements,box,z0,z1,radius=0,rows=1):
    u0,v0,u1,v1=box
    for i in range(rows):
        top=v0+(v1-v0)*i/rows;bottom=v0+(v1-v0)*(i+1)/rows
        near=min((top+bottom)/2-v0,v1-(top+bottom)/2)
        inset=radius-math.sqrt(max(0,radius*radius-(radius-near)**2)) if near<radius else 0
        lo,hi=u0+inset,u1-inset
        elements.append(cuboid(X(hi),Y(bottom),z0,X(lo),Y(top),z1,front_uv=[lo,top,hi,bottom]))

for off in [False,True]:
    elements=[]
    # Recessed wall-side casing, then a bevel rim and curved front shell.
    panel(elements,(158,10,501,501),14.1,16,19,28)
    panel(elements,(151,5,507,505),13.58,15.7,20,44)
    panel(elements,(155,9,503,501),13.3,13.6,18,44)
    panel(elements,(20,24,157,492),12.9,15.6,17,32)
    # Raised grille platform and deep, dished receiver seats, visible off-hook.
    panel(elements,(31,177,143,318),12.57,12.94,12,18)
    panel(elements,(29,34,140,64),12.3,12.92,10,6)
    panel(elements,(29,64,43,156),12.3,12.92,4,8)
    panel(elements,(128,64,140,156),12.3,12.92,4,8)
    panel(elements,(31,354,143,375),12.35,12.94,8,5)
    panel(elements,(31,375,43,478),12.35,12.94,4,8)
    panel(elements,(131,375,143,478),12.35,12.94,4,8)
    panel(elements,(40,466,135,480),12.35,12.94,5,4)
    panel(elements,(66,48,103,91),12.18,12.92,2,4)
    # Black raised bezel: glass remains recessed on the 13.3 front plane, leaving
    # its exact window uncovered for the live monochrome video renderer.
    for box in [(172,25,489,42),(172,42,200,211),(386,42,489,211),(172,202,489,218)]:
        panel(elements,box,13.12,13.32,3,6)
    for yy,rad in [(286,14),(330,14),(374,14),(431,18)]:
        for i in range(10):
            top=yy-rad+2*rad*i/10;bottom=yy-rad+2*rad*(i+1)/10
            half=math.sqrt(max(0,rad*rad-((top+bottom)/2-yy)**2))
            panel(elements,(467-half,top,467+half,bottom),13.015,13.31)
    # Smooth bent receiver, a rounded cross-section and a narrow handle. The
    # underside bows away from the cradle; lifted receiver exposes inner holes.
    dx,dy,dz=(4.2,1,-1.8) if off else (0,0,0)
    receiver_start=len(elements)
    for i in range(48):
        t=(i+.5)/48;v0=3+505*i/48;v1=3+505*(i+1)/48
        half=receiver_half(t)*116/128
        centre=61
        for j in range(6):
            q0=-1+j/3;q1=-1+(j+1)/3;qm=(q0+q1)/2
            u0=centre+half*q0;u1=centre+half*q1
            # bulging outer face, rounded shoulders, deep at both ends
            zfront=10.62+.42*abs(qm)**2 + .16*math.sin(math.pi*t)
            c=cuboid(X(u1)+dx,Y(v1)+dy,zfront+dz,X(u0)+dx,Y(v0)+dy,12.5+dz,texture='#body')
            c['faces']['north']={'texture':'#receiver' if off else '#outer','uv':[(q0+1)*8,v0/32,(q1+1)*8,v1/32]}
            # Keep shoulder/riser faces closed between unequal curved sections.
            elements.append(c)
    model={'ambientocclusion':True,'textures':{'front':'homesystems:block/sht_308_mesh','body':'homesystems:block/sht_308_side','receiver':'homesystems:block/sht_308_receiver_mesh','outer':'homesystems:block/sht_308_receiver_outer_mesh','particle':'homesystems:block/sht_308_side'},'elements':elements,'display':{'gui':{'rotation':[12,165,0],'scale':[.85,.85,.85]},'fixed':{'rotation':[0,180,0]},'firstperson_righthand':{'rotation':[0,135,0],'scale':[.6,.6,.6]},'thirdperson_righthand':{'rotation':[75,45,0],'scale':[.5,.5,.5]}}}
    if not off:
        write(A/'models/item/sht_308.json',model)
    # The placed receiver uses a continuous curved surface in the block-entity
    # renderer. Keep its baked fallback only for the inventory item.
    model['elements']=elements[:receiver_start]
    write(A/f'models/block/sht_308{"_offhook" if off else ""}.json',model)
write(A/'blockstates/sht_308.json',{'variants':{f'facing={face},offhook={str(off).lower()}':{'model':'homesystems:block/sht_308'+('_offhook' if off else ''),'y':angle} for face,angle in [('north',0),('east',90),('south',180),('west',270)] for off in [False,True]}})
write(DATA/'recipes/sht_308.json',{'type':'minecraft:crafting_shaped','category':'redstone','pattern':['III','RGR','ICI'],'key':{'I':{'item':'minecraft:iron_ingot'},'R':{'item':'minecraft:redstone'},'G':{'item':'minecraft:glass_pane'},'C':{'item':'minecraft:quartz'}},'result':{'item':'homesystems:sht_308'}})
write(DATA/'loot_tables/blocks/sht_308.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'homesystems:sht_308'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
lamp=Art(32,32);lamp.oval((2,2,29,29),'white');lamp.save('panel_lamp.png')
print('Rebuilt SHT-308: bevelled shell, recessed CRT, grille/hook/seats, curved cord-free receiver.')
