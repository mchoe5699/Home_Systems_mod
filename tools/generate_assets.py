"""Regenerate the original pixel-art device assets (requires Pillow)."""
import json
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
ASSETS = ROOT / 'assets/homesystems'
DATA = ROOT / 'data'
DEVICES = {
    'lobby_phone': ('로비폰', 'Lobby Phone', '#505d68', '#58cbbc', 'minecraft:stone_button'),
    'video_phone': ('비디오폰', 'Video Phone', '#deded8', '#6ccce4', 'minecraft:glass_pane'),
    'interphone': ('인터폰', 'Interphone', '#e5d9bd', '#c5d9aa', 'minecraft:note_block'),
    'wallpad': ('월패드', 'Wallpad', '#eceff2', '#54d6c0', 'minecraft:quartz'),
    'guard_station': ('경비실기', 'Guard Station', '#344654', '#efbb65', 'minecraft:compass'),
    'door_camera': ('도어카메라', 'Door Camera', '#a7b5be', '#66d7c1', 'minecraft:spyglass'),
    'door_phone': ('도어폰', 'Door Phone', '#d9d4c7', '#e7bd69', 'minecraft:bell'),
}

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

for name, (ko, en, body, accent, ingredient) in DEVICES.items():
    im = Image.new('RGB', (32, 32), body)
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 31, 31), outline='#17222a')
    d.line((1, 1, 30, 1), fill='#ffffff')
    d.line((1, 1, 1, 30), fill='#abb6bc')
    d.line((30, 2, 30, 30), fill='#263641')
    for x in (3, 28):
        for y in (3, 28):
            d.rectangle((x-1, y-1, x, y), fill='#7b858a')
            d.point((x, y), fill='#303a41')
    if name == 'lobby_phone':
        d.rectangle((6, 3, 25, 9), fill='#202c36')
        d.ellipse((13, 4, 18, 8), fill='#090f18', outline='#82969f')
        d.point((15, 5), fill='#5bbedf')
        d.rectangle((6, 11, 25, 16), fill='#152e31', outline='#7c8d91')
        d.line((9, 13, 21, 13), fill=accent)
        for x in (9, 15, 21):
            for y in (20, 24, 28):
                d.rectangle((x-1, y-1, x+2, y+1), fill='#a6b4bc', outline='#263640')
        d.rectangle((24, 20, 26, 27), fill=accent)
    elif name == 'door_camera':
        d.rectangle((7,4,24,15),fill='#24343f',outline='#718993')
        d.ellipse((11,5,20,14),fill='#08111b',outline='#c0d0da')
        d.ellipse((14,8,17,11),fill='#38768f');d.point((14,8),fill='#a1e9ec')
        for y in (18,20):d.line((8,y,23,y),fill='#485d68')
        d.rounded_rectangle((11,24,20,28),radius=1,fill=accent,outline='#304d59')
    elif name == 'door_phone':
        d.rectangle((7,4,24,17),fill='#424a49',outline='#86938c')
        for y in range(6,16,2):d.line((9,y,22,y),fill='#202e32')
        d.rounded_rectangle((10,23,21,28),radius=2,fill=accent,outline='#7b704c')
    elif name == 'interphone':
        d.rounded_rectangle((5, 4, 12, 27), radius=2, fill='#f5eedc', outline='#7b776b')
        d.rectangle((7, 9, 11, 22), fill=body)
        d.rectangle((16, 6, 26, 12), fill='#384436', outline='#9a9b83')
        for y in (16, 18, 20): d.line((16, y, 25, y), fill='#656b61')
        d.rectangle((18, 24, 24, 27), fill=accent, outline='#5a6956')
    else:
        left = 12 if name in ('video_phone', 'guard_station') else 4
        if left == 12:
            d.rounded_rectangle((3, 5, 10, 27), radius=2, fill='#a9b6bc' if name == 'guard_station' else '#ffffff', outline='#657680')
            d.rectangle((5, 10, 9, 22), fill=body)
        d.rectangle((left, 5, 27, 22), fill='#091923', outline='#778b97')
        d.rectangle((left+2, 7, 25, 19), fill='#163645')
        d.rectangle((left+2, 7, 25, 9), fill=accent)
        d.rectangle((left+3, 13, left+6, 17), fill='#6ba1a4')
        d.line((left+8, 13, 24, 13), fill='#77949d')
        d.line((left+8, 16, 23, 16), fill='#526f7c')
        for x in range(left+1, 26, 5):
            d.rectangle((x, 25, x+2, 27), fill=accent, outline='#627982')
    path = ASSETS / f'textures/block/{name}.png'
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)
    texture = f'homesystems:block/{name}'
    faces = {face: {'texture': '#body', 'uv': [0, 0, 16, 16]} for face in ['up','down','east','west','south']}
    faces['north'] = {'texture': '#front', 'uv': [0, 0, 16, 16]}
    elements = [{'from':[2,1,13], 'to':[14,15,16], 'faces':faces}]
    # Raised handset/hood gives the five terminals physical depth.
    if name in ('video_phone','interphone','guard_station'):
        for y1,y2 in ((3,6),(11,13)):
            elements.append({'from':[3,y1,12], 'to':[5.5,y2,13], 'faces':{f:{'texture':'#body'} for f in ['north','south','east','west','up','down']}})
    if name in ('lobby_phone','door_camera'):
        elements.append({'from':[4,14,12], 'to':[12,15,16], 'faces':{f:{'texture':'#body'} for f in ['north','south','east','west','up','down']}})
    model = {'ambientocclusion':False, 'textures':{'front':texture,'body':'minecraft:block/smooth_stone','particle':texture},
        'elements':elements, 'display':{
            'gui':{'rotation':[15,160,0],'translation':[0,0,0],'scale':[0.95,0.95,0.95]},
            'fixed':{'rotation':[0,180,0],'translation':[0,0,-5],'scale':[1,1,1]},
            'thirdperson_righthand':{'rotation':[75,45,0],'translation':[0,2.5,0],'scale':[0.45,0.45,0.45]},
            'firstperson_righthand':{'rotation':[0,135,0],'translation':[0,0,0],'scale':[0.6,0.6,0.6]},
            'ground':{'translation':[0,3,0],'scale':[0.5,0.5,0.5]}}}
    write(ASSETS/f'models/block/{name}.json',model)
    write(ASSETS/f'models/item/{name}.json',{'parent':f'homesystems:block/{name}'})
    write(ASSETS/f'blockstates/{name}.json',{'variants':{f'facing={face}':dict(model=f'homesystems:block/{name}', **({'y':angle} if angle else {})) for face,angle in [('north',0),('east',90),('south',180),('west',270)]}})
    write(DATA/f'homesystems/loot_tables/blocks/{name}.json', {'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'homesystems:{name}'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    write(DATA/f'homesystems/recipes/{name}.json',{'type':'minecraft:crafting_shaped','category':'redstone','pattern':['III','RGR','ICI'],'key':{'I':{'item':'minecraft:iron_ingot'},'R':{'item':'minecraft:redstone'},'G':{'item':ingredient},'C':{'item':'minecraft:copper_ingot'}},'result':{'item':f'homesystems:{name}'}})
write(DATA/'minecraft/tags/blocks/mineable/pickaxe.json', {'replace':False,'values':[f'homesystems:{name}' for name in DEVICES]})

ko = {
'itemGroup.homesystems':'홈시스템',
'screen.homesystems.subtitle':'공동주택 통신 시스템',
'screen.homesystems.own_number':'내 기기 번호', 'screen.homesystems.target_number':'호출할 번호',
'screen.homesystems.save':'번호 등록 / 저장', 'screen.homesystems.call':'호출',
'screen.homesystems.answer':'응답', 'screen.homesystems.end':'종료', 'screen.homesystems.unlock':'문 열기',
'screen.homesystems.idle':'● 대기 중', 'screen.homesystems.outgoing':'● 호출 중',
'screen.homesystems.ringing':'● 수신 중', 'screen.homesystems.connected':'● 연결됨',
'screen.homesystems.peer':'상대 %s',
'screen.homesystems.video_tab':'영상 화면', 'screen.homesystems.controls_tab':'통신 조작',
'screen.homesystems.live':'● 실시간 영상', 'screen.homesystems.no_video':'연결된 카메라 대기 중',
'screen.homesystems.hint':'번호 등록 → 상대 번호 입력 → 호출 → 응답',
'screen.homesystems.text_hint':'연결 후 채팅 버튼 또는 T키로 대화',
'message.homesystems.owner_only':'[홈시스템] 설치자 또는 관리자만 설정·문 열기가 가능합니다.',
'message.homesystems.saved':'[홈시스템] 기기 번호와 기본 호출 번호를 저장했습니다.',
'message.homesystems.invalid_number':'[홈시스템] 1~12자리 숫자를 입력하세요. 중복 번호나 통화 중 변경은 사용할 수 없습니다.',
'message.homesystems.invalid_target':'[홈시스템] 호출 번호는 1~12자리 숫자로 입력하세요.',
'message.homesystems.configure_first':'[홈시스템] 먼저 내 기기 번호를 등록하세요.',
'message.homesystems.offline':'[홈시스템] 해당 기기를 찾을 수 없습니다. 번호와 청크 로딩 여부를 확인하세요.',
'message.homesystems.busy':'[홈시스템] 통화 중이거나 자기 자신을 호출할 수 없습니다.',
'message.homesystems.calling':'[홈시스템] 호출 중입니다. 상대 기기에서 응답해 주세요.',
'message.homesystems.no_call':'[홈시스템] 응답할 수 있는 호출이 없습니다.',
'message.homesystems.unlocked':'[홈시스템] 상대 출입 기기의 문 열림 신호를 5초간 켰습니다.',
'message.homesystems.cannot_unlock':'[홈시스템] 연결된 상대가 로비폰·인터폰이어야 합니다. 이미 열린 문은 잠시 기다리세요.',
'message.homesystems.incoming':'[홈시스템] %s번에서 호출합니다! 기기를 우클릭해 응답하세요.',
'message.homesystems.connected':'[홈시스템] %s번과 연결되었습니다. 기기 8블록 안에서 일반 채팅으로 대화하세요.',
'message.homesystems.no_connected_device':'[홈시스템] 8블록 안에 연결된 기기가 없거나 메시지가 비어 있습니다(최대 256자).',
'message.homesystems.chat':'[인터폰 %s] %s: %s', 'message.homesystems.sent':'[홈시스템] %s번 기기에 메시지를 보냈습니다.',
}
en = {
'itemGroup.homesystems':'Home Systems',
'screen.homesystems.subtitle':'APARTMENT COMMUNICATION',
'screen.homesystems.own_number':'Device number', 'screen.homesystems.target_number':'Call number',
'screen.homesystems.save':'Register / Save', 'screen.homesystems.call':'Call',
'screen.homesystems.answer':'Answer', 'screen.homesystems.end':'End', 'screen.homesystems.unlock':'Open door',
'screen.homesystems.idle':'● Standby', 'screen.homesystems.outgoing':'● Calling',
'screen.homesystems.ringing':'● Incoming call', 'screen.homesystems.connected':'● Connected',
'screen.homesystems.peer':'Peer %s',
'screen.homesystems.video_tab':'Camera view', 'screen.homesystems.controls_tab':'Controls',
'screen.homesystems.live':'● LIVE CAMERA', 'screen.homesystems.no_video':'Waiting for camera signal',
'screen.homesystems.hint':'Register → Dial → Call → Answer',
'screen.homesystems.text_hint':'Connected: Chat button or T to talk',
'message.homesystems.owner_only':'[Home Systems] Only the installer or an operator can configure or unlock.',
'message.homesystems.saved':'[Home Systems] Device number and default call number saved.',
'message.homesystems.invalid_number':'[Home Systems] Use 1–12 digits. Numbers must be unique; end the call before changing settings.',
'message.homesystems.invalid_target':'[Home Systems] The call number must have 1–12 digits.',
'message.homesystems.configure_first':'[Home Systems] Register this device number first.',
'message.homesystems.offline':'[Home Systems] Device not found. Check the number and whether its chunk is loaded.',
'message.homesystems.busy':'[Home Systems] A device is busy, or you tried to call yourself.',
'message.homesystems.calling':'[Home Systems] Calling. Answer at the other device.',
'message.homesystems.no_call':'[Home Systems] No incoming call to answer.',
'message.homesystems.unlocked':'[Home Systems] Remote entrance relay activated for 5 seconds.',
'message.homesystems.cannot_unlock':'[Home Systems] Connect to a lobby phone or interphone. An active relay must reset first.',
'message.homesystems.incoming':'[Home Systems] Incoming call from %s! Right-click the device to answer.',
'message.homesystems.connected':'[Home Systems] Connected to %s. Use normal chat within 8 blocks.',
'message.homesystems.no_connected_device':'[Home Systems] No connected device within 8 blocks, or an empty/oversized message (max 256 characters).',
'message.homesystems.chat':'[Intercom %s] %s: %s', 'message.homesystems.sent':'[Home Systems] Message sent to device %s.',
}
for name,(k,e,*_) in DEVICES.items():
    ko[f'block.homesystems.{name}']=k
    en[f'block.homesystems.{name}']=e
ko_extra = {
    'screen.chat':'채팅', 'screen.home_tab':'홈 네트워크', 'screen.home_subtitle':'세대 내 장치 제어',
    'screen.home_title':'홈 네트워크 연결 상태', 'screen.channel_lights':'조명', 'screen.channel_door':'문',
    'screen.channel_gas':'가스 밸브', 'screen.channel_other':'기타 장치', 'screen.relay_count':'연결 %s대',
    'screen.home_on':'켜기 / 열기', 'screen.home_off':'끄기 / 닫기', 'screen.gas_on':'가스 공급', 'screen.gas_off':'가스 차단',
    'screen.state_on':'켜짐', 'screen.state_off':'꺼짐',
    'screen.home_hint':'연결기로 월패드 → 릴레이를 우클릭',
    'screen.home_hint2':'같은 단지 · 64블록 이내 · 최대 32대',
    'message.relay_hint':'[홈시스템] 네트워크 연결기로 월패드를 선택한 뒤 이 릴레이를 연결하세요.',
    'message.wallpad_selected':'[홈시스템] 월패드 선택 완료. 연결할 릴레이를 우클릭하세요.',
    'message.relay_unlinked':'[홈시스템] 릴레이 연결을 해제했습니다.',
    'message.select_wallpad':'[홈시스템] 연결기로 월패드를 먼저 우클릭해 선택하세요.',
    'message.network_range':'[홈시스템] 같은 단지·같은 차원의 64블록 이내에 최대 32대까지 연결할 수 있습니다.',
    'message.wallpad_offline':'[홈시스템] 선택한 월패드의 청크가 로드되지 않았습니다.',
    'message.relay_linked':'[홈시스템] 릴레이 연결 완료! 월패드의 홈 네트워크에서 제어하세요.',
    'message.linker_hint':'[홈시스템] 월패드 → 릴레이 순서로 우클릭. Shift+릴레이 우클릭은 연결 해제.',
    'message.no_relays':'[홈시스템] 이 채널에 연결된 릴레이가 없습니다.',
}
en_extra = {
    'screen.chat':'Chat', 'screen.home_tab':'Home network', 'screen.home_subtitle':'HOME DEVICE CONTROL',
    'screen.home_title':'HOME NETWORK', 'screen.channel_lights':'Lighting', 'screen.channel_door':'Doors',
    'screen.channel_gas':'Gas valve', 'screen.channel_other':'Other devices', 'screen.relay_count':'%s linked',
    'screen.home_on':'Enable', 'screen.home_off':'Disable', 'screen.gas_on':'Gas supply', 'screen.gas_off':'Shut off gas',
    'screen.state_on':'ON', 'screen.state_off':'OFF',
    'screen.home_hint':'Linker: right-click wallpad, then relay',
    'screen.home_hint2':'Same estate, 64 blocks, max 32 relays',
    'message.relay_hint':'[Home Systems] Select a wallpad with the Network Linker, then link this relay.',
    'message.wallpad_selected':'[Home Systems] Wallpad selected. Right-click the relay to connect.',
    'message.relay_unlinked':'[Home Systems] Relay disconnected.',
    'message.select_wallpad':'[Home Systems] First select a wallpad with the Network Linker.',
    'message.network_range':'[Home Systems] Up to 32 relays within 64 blocks in the same estate and dimension.',
    'message.wallpad_offline':'[Home Systems] The selected wallpad chunk is not loaded.',
    'message.relay_linked':'[Home Systems] Relay connected! Use the wallpad Home network screen.',
    'message.linker_hint':'[Home Systems] Right-click wallpad, then relay. Shift+right-click relay to disconnect.',
    'message.no_relays':'[Home Systems] No relays linked to this channel.',
}
for target, extra in [(ko,ko_extra),(en,en_extra)]:
    for key,value in extra.items():
        group,name=key.split('.',1)
        target[f'{group}.homesystems.{name}']=value

relays = {
    'light_relay':('조명 릴레이','Lighting Relay','#edcf62','minecraft:glowstone_dust'),
    'door_relay':('문 릴레이','Door Relay','#6dbad5','minecraft:iron_door'),
    'gas_relay':('가스 밸브 릴레이','Gas Valve Relay','#e98656','minecraft:lever'),
    'utility_relay':('기타 장치 릴레이','Utility Relay','#a68ad9','minecraft:quartz'),
}
for name,(k,e,color,ingredient) in relays.items():
    ko[f'block.homesystems.{name}']=k;en[f'block.homesystems.{name}']=e
    for powered in [False,True]:
        suffix='_on' if powered else ''
        im=Image.new('RGB',(16,16),'#d7dce0');d=ImageDraw.Draw(im)
        d.rectangle((0,0,15,15),outline='#33444f');d.rectangle((2,3,13,12),fill='#243640',outline='#82929b')
        d.rectangle((4,5,11,7),fill=color)
        d.rectangle((4,10,6,11),fill='#54f7ac' if powered else '#485a64')
        for x in [3,12]:d.point((x,2),fill='#71828b');d.point((x,13),fill='#71828b')
        path=ASSETS/f'textures/block/{name}{suffix}.png';path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
        write(ASSETS/f'models/block/{name}{suffix}.json',{'parent':'minecraft:block/cube_all','textures':{'all':f'homesystems:block/{name}{suffix}'}})
    write(ASSETS/f'blockstates/{name}.json',{'variants':{'powered=false':{'model':f'homesystems:block/{name}'},'powered=true':{'model':f'homesystems:block/{name}_on'}}})
    write(ASSETS/f'models/item/{name}.json',{'parent':f'homesystems:block/{name}'})
    write(DATA/f'homesystems/loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'homesystems:{name}'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    write(DATA/f'homesystems/recipes/{name}.json',{'type':'minecraft:crafting_shaped','category':'redstone','pattern':['ICI','RGR','III'],'key':{'I':{'item':'minecraft:iron_ingot'},'R':{'item':'minecraft:redstone'},'C':{'item':'minecraft:copper_ingot'},'G':{'item':ingredient}},'result':{'item':f'homesystems:{name}'}})
ko['item.homesystems.network_linker']='네트워크 연결기';en['item.homesystems.network_linker']='Network Linker'
im=Image.new('RGBA',(16,16),(0,0,0,0));d=ImageDraw.Draw(im)
d.line((3,14,10,7),fill='#233b4a',width=5);d.line((3,14,10,7),fill='#53d4bc',width=2)
d.rectangle((8,2,14,7),fill='#e2e9ed',outline='#3c505d');d.rectangle((10,3,12,5),fill='#55c1d9')
path=ASSETS/'textures/item/network_linker.png';path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
write(ASSETS/'models/item/network_linker.json',{'parent':'minecraft:item/handheld','textures':{'layer0':'homesystems:item/network_linker'}})
write(DATA/'homesystems/recipes/network_linker.json',{'type':'minecraft:crafting_shaped','category':'redstone','pattern':[' IC',' RI','R  '],'key':{'I':{'item':'minecraft:iron_ingot'},'R':{'item':'minecraft:redstone'},'C':{'item':'minecraft:copper_ingot'}},'result':{'item':'homesystems:network_linker'}})
write(DATA/'minecraft/tags/blocks/mineable/pickaxe.json',{'replace':False,'values':[f'homesystems:{name}' for name in [*DEVICES,*relays]]})
write(ASSETS/'lang/ko_kr.json',ko)
write(ASSETS/'lang/en_us.json',en)
dashboard_ko = {
 'new_name':'새 단지', 'name':'단지 이름', 'rename':'이름 저장', 'create':'구획 만들기', 'merge':'단지 합치기', 'split':'구획 분리',
 'creating':'드래그해서 생성', 'merging':'두 단지 클릭', 'splitting':'분리할 구획 클릭', 'delete':'구획 삭제', 'confirm':'한 번 더 눌러 삭제', 'center':'내 위치 / 새로고침',
 'loading':'지도를 불러오는 중...', 'selected':'단지를 선택했습니다.', 'mode_select':'지도를 클릭해 단지를 선택하세요.',
 'mode_create':'이름 입력 후 지도에서 마우스를 눌러 끌어 구획을 만드세요.', 'mode_merge':'합칠 첫 단지를 선택하고 두 번째 단지를 클릭하세요.',
 'mode_split':'원래 구획을 클릭하면 독립된 단지로 분리됩니다.', 'delete_confirm':'선택한 구획을 삭제하려면 삭제 버튼을 다시 누르세요.',
 'map_hint':'우드래그: 이동 · 휠: 확대 · 1칸=16블록',
 'created':'단지 구획을 만들었습니다.', 'renamed':'단지 이름을 저장했습니다.', 'merged':'합쳤습니다. 맞닿은 내부 경계만 없어집니다.',
 'split':'구획을 분리했습니다.', 'deleted':'구획을 삭제했습니다.', 'bad_name':'단지 이름을 1~32자로 입력하세요.', 'too_large':'구획은 최대 64×64칸이며 전체 512개까지 가능합니다.',
 'overlap':'다른 구획과 겹칩니다. 빈 곳에 구획을 만드세요.', 'not_owner':'구획 생성자 또는 관리자만 편집할 수 있습니다.',
 'different_estates':'서로 다른 두 단지를 선택하세요.', 'single_parcel':'합쳐진 구획이 없어 분리할 수 없습니다.', 'wait':'잠시 후 다시 시도하세요.',
}
dashboard_en = {
 'new_name':'New estate', 'name':'Estate name', 'rename':'Save name', 'create':'Create parcel', 'merge':'Merge estates', 'split':'Split parcel',
 'creating':'Drag to create', 'merging':'Click two estates', 'splitting':'Click original part', 'delete':'Delete parcel', 'confirm':'Confirm deletion', 'center':'Center / Refresh',
 'loading':'Loading terrain map...', 'selected':'Estate selected.', 'mode_select':'Click an estate to select it.',
 'mode_create':'Enter a name, then drag on the map to create a parcel.', 'mode_merge':'Select the first estate, then click the second.',
 'mode_split':'Click an original parcel to detach it from its estate.', 'delete_confirm':'Click Delete again to remove the selected parcel.',
 'map_hint':'Right-drag: pan | Wheel: zoom | Cell: 16 blocks',
 'created':'Estate parcel created.', 'renamed':'Estate name saved.', 'merged':'Merged. Only shared internal borders disappear.',
 'deleted':'Parcel deleted.', 'bad_name':'Use an estate name with 1-32 characters.', 'too_large':'Maximum 64x64 cells per parcel; 512 parcels in total.',
 'overlap':'That area overlaps an existing parcel.', 'not_owner':'Only the creator or an operator can edit this estate.',
 'different_estates':'Choose two different estates.', 'single_parcel':'There are no merged parcels to split.', 'wait':'Please wait briefly and try again.',
}
# Use a distinct result key so the split button keeps its short caption.
dashboard_ko['split_done']='구획을 분리했습니다.'; dashboard_ko['split']='구획 분리'
dashboard_en['split_done']='Parcel detached into a separate estate.'
for language, values in [(ko,dashboard_ko),(en,dashboard_en)]:
    language.update({f'dashboard.homesystems.{key}':value for key,value in values.items()})
ko['item.homesystems.estate_dashboard']='단지 대시보드';en['item.homesystems.estate_dashboard']='Estate Dashboard'
ko['message.homesystems.outside_estate']='[홈시스템] 기기와 사용자가 같은 단지 구획 안에 있어야 합니다. 단지 대시보드로 구획을 만드세요.'
en['message.homesystems.outside_estate']='[Home Systems] You and the device must be inside the same estate. Create parcels with the Estate Dashboard.'
ko['message.homesystems.same_estate']='[홈시스템] 서로 다른 단지의 기기는 연결할 수 없습니다. 대시보드에서 단지를 합치세요.'
en['message.homesystems.same_estate']='[Home Systems] Devices in different estates cannot connect. Merge the estates in the dashboard.'
write(ASSETS/'lang/ko_kr.json',ko);write(ASSETS/'lang/en_us.json',en)
im=Image.new('RGBA',(16,16),(0,0,0,0));d=ImageDraw.Draw(im)
d.rounded_rectangle((1,1,14,14),radius=2,fill='#d0dce2',outline='#304856');d.rectangle((3,3,12,11),fill='#223b44')
d.rectangle((4,4,7,7),fill='#6cba85');d.rectangle((8,4,11,7),fill='#5ba1bd');d.rectangle((4,8,11,10),fill='#acbf78')
d.line((7,4,7,10),fill='#f8dc8b');d.line((4,7,11,7),fill='#f8dc8b');d.rectangle((7,12,8,12),fill='#56d7be')
im.save(ASSETS/'textures/item/estate_dashboard.png')
write(ASSETS/'models/item/estate_dashboard.json',{'parent':'minecraft:item/generated','textures':{'layer0':'homesystems:item/estate_dashboard'}})
write(DATA/'homesystems/recipes/estate_dashboard.json',{'type':'minecraft:crafting_shaped','category':'misc','pattern':['III','GMR','ICI'],'key':{'I':{'item':'minecraft:iron_ingot'},'G':{'item':'minecraft:glass_pane'},'M':{'item':'minecraft:map'},'R':{'item':'minecraft:redstone'},'C':{'item':'minecraft:compass'}},'result':{'item':'homesystems:estate_dashboard'}})
print('Generated textures, models, blockstates, recipes, loot and translations for', len(DEVICES), 'devices.')

address_ko={
 'address_settings':'관할경비실 및 세대번호(동·호) 설정', 'guard_settings':'경비실번호 설정',
 'assigned_guard':'관할 경비실', 'guard_number':'경비실 번호', 'building':'동 번호', 'unit':'호 번호',
 'dial_building':'호출할 동', 'dial_unit':'호출할 호', 'dial_guard':'다른 경비실 번호',
 'save_address':'입력한 번호 저장', 'address_saved_button':'저장됨', 'back':'조작 화면으로 돌아가기',
 'settings_subtitle':'번호를 직접 입력한 뒤 저장하세요', 'address_range':'경비실·동·호 번호: 각각 0~9999',
 'outside_lobby_hint':'외곽 로비폰은 동 번호를 9999로 설정', 'not_configured':'번호 미설정 · 설정 버튼에서 등록하세요',
 'guard_address':'경비실 %s번', 'home_address':'%s동 %s호 · 관할 경비실 %s번',
 'call_home':'세대 호출', 'priority_guard':'관할 경비실 우선 호출', 'other_guard':'입력한 다른 경비실 호출',
 'ring_doorbell':'우리 세대 호출벨', 'audio_only':'채팅 통화 · 상대 기기는 영상 없음',
}
address_en={
 'address_settings':'Guard office & household settings', 'guard_settings':'Guard office number settings',
 'assigned_guard':'Assigned guard', 'guard_number':'Guard office no.', 'building':'Building no.', 'unit':'Unit no.',
 'dial_building':'Call building', 'dial_unit':'Call unit', 'dial_guard':'Other guard no.',
 'save_address':'Save entered numbers', 'address_saved_button':'Saved', 'back':'Back to controls',
 'settings_subtitle':'Enter the numbers, then save', 'address_range':'Each number: 0-9999 (including zero)',
 'outside_lobby_hint':'Perimeter lobby: set building to 9999', 'not_configured':'Not configured - open address settings',
 'guard_address':'Guard office %s', 'home_address':'Building %s / Unit %s / Guard %s',
 'call_home':'Call household', 'priority_guard':'Assigned guard first', 'other_guard':'Call entered other guard',
 'ring_doorbell':'Ring own household', 'audio_only':'CHAT CALL - NO REMOTE VIDEO',
}
for lang,extra in [(ko,address_ko),(en,address_en)]:lang.update({f'screen.homesystems.{k}':v for k,v in extra.items()})
ko['message.homesystems.address_saved']='[홈시스템] 관할 경비실·동·호 번호를 저장했습니다.'
en['message.homesystems.address_saved']='[Home Systems] Guard office and household address saved.'
ko['message.homesystems.address_invalid']='[홈시스템] 각 번호를 0~9999로 입력하세요. 통화 중에는 설정을 바꿀 수 없습니다.'
en['message.homesystems.address_invalid']='[Home Systems] Each number must be 0-9999. End the call before changing settings.'
ko['message.homesystems.address_offline']='[홈시스템] 호출 가능한 기기가 없습니다. 단지·관할 경비실·동·호·청크 로딩과 통화 상태를 확인하세요.'
en['message.homesystems.address_offline']='[Home Systems] No eligible idle device. Check estate, jurisdiction, address and loaded chunks.'
ko['message.homesystems.configure_first']='[홈시스템] 먼저 설정 버튼에서 경비실 번호 또는 관할 경비실·동·호 번호를 입력하고 저장하세요.'
en['message.homesystems.configure_first']='[Home Systems] Open settings, enter and save the guard office or household address first.'
ko['message.homesystems.cannot_unlock']='[홈시스템] 통화 상대가 로비폰·도어카메라·도어폰이어야 합니다. 이미 열린 문은 잠시 기다리세요.'
en['message.homesystems.cannot_unlock']='[Home Systems] Connect to a lobby phone, door camera or door phone. Wait for an active relay to reset.'
write(ASSETS/'lang/ko_kr.json',ko);write(ASSETS/'lang/en_us.json',en)
for language,values in [(ko,{'presence_0':'근무중','presence_1':'부재','presence_2':'외출','no_bypass_hint':'관할 경비실이 부재·외출일 때만 우회 가능'}),(en,{'presence_0':'Present','presence_1':'Away','presence_2':'Out','no_bypass_hint':'Alternate office: assigned guard must be Away/Out'})]:language.update({f'screen.homesystems.{k}':v for k,v in values.items()})
ko['message.homesystems.bypass_forbidden']='[홈시스템] 관할 경비실이 부재 또는 외출로 설정된 경우에만 다른 경비실로 우회할 수 있습니다. 통화 중·청크 미로딩·미등록은 부재가 아닙니다.'
en['message.homesystems.bypass_forbidden']='[Home Systems] Alternate offices require an explicit Away/Out status at every assigned office. Busy, unloaded and missing are not Away.'
ko['message.homesystems.presence_saved']='[홈시스템] 경비실 근무 상태를 저장했습니다.'
en['message.homesystems.presence_saved']='[Home Systems] Guard presence status saved.'
write(ASSETS/'lang/ko_kr.json',ko);write(ASSETS/'lang/en_us.json',en)
link_texts_ko={
 'home_source_selected':'홈 링크: 내부 기기를 선택했습니다. 연결할 도어카메라·도어폰을 우클릭하세요.',
 'door_source_selected':'도어 링크: 제어 기기를 선택했습니다. 연결할 문을 우클릭하세요.',
 'select_indoor':'홈 링크로 비디오폰·월패드·인터폰을 먼저 선택하세요.',
 'select_door_controller':'도어 링크로 세대 내부 기기 또는 로비폰을 먼저 선택하세요.',
 'select_door_device':'홈 링크의 연결 대상은 도어카메라 또는 도어폰입니다.',
 'home_link_removed':'홈 링크 연결을 해제했습니다.', 'home_link_added':'홈 링크 연결 완료. 세대 주소를 적용하고 선택한 내부 기기를 연결했습니다.',
 'door_link_removed':'문 연결을 해제했습니다.', 'door_link_added':'문 연결 완료. 기기의 문 열기 버튼으로 실제 문을 제어할 수 있습니다.',
 'link_missing':'선택한 기기와 연결된 대상이 아닙니다.',
 'physical_link_invalid':'같은 단지·같은 차원·64블록 이내에서 연결하세요(종류별 최대 16개). 홈 링크는 통화 종료 후 연결하세요.',
 'home_link_required':'홈 링크로 내부 기기와 먼저 연결하세요.',
 'unlocked':'연결된 문 또는 문 열림 신호를 작동했습니다.',
 'cannot_unlock':'도어 링크 연결 또는 통화 상대의 현관 연결을 확인하세요.',
}
link_texts_en={
 'home_source_selected':'Home Link: indoor device selected. Right-click a door camera or door phone.',
 'door_source_selected':'Door Link: controller selected. Right-click a door.',
 'select_indoor':'First select a video phone, wallpad or interphone with Home Link.',
 'select_door_controller':'First select an indoor device or lobby phone with Door Link.',
 'select_door_device':'Home Link targets a door camera or door phone.',
 'home_link_removed':'Home Link disconnected.', 'home_link_added':'Home Link connected; household address applied to the entrance device.',
 'door_link_removed':'Door disconnected.', 'door_link_added':'Door connected. Use the device Open door button.',
 'link_missing':'This target is not linked to the selected controller.',
 'physical_link_invalid':'Use the same estate/dimension within 64 blocks (max 16 per kind). End calls before home pairing.',
 'home_link_required':'First connect an indoor device using Home Link.',
 'unlocked':'Linked doors or the entrance relay activated.',
 'cannot_unlock':'Check the Door Link or the connected entrance device.',
}
for language,values in [(ko,link_texts_ko),(en,link_texts_en)]:language.update({f'message.homesystems.{k}':'[홈시스템] '+v if language is ko else '[Home Systems] '+v for k,v in values.items()})
for name,k,e,color,ingredient in [('door_link','도어 링크','Door Link','#65bce2','minecraft:iron_door'),('home_link','홈 링크','Home Link','#eca66a','minecraft:quartz')]:
 ko[f'item.homesystems.{name}']=k;en[f'item.homesystems.{name}']=e
 im=Image.new('RGBA',(16,16),(0,0,0,0));d=ImageDraw.Draw(im)
 d.line((3,13,10,6),fill='#263d4b',width=5);d.line((3,13,10,6),fill=color,width=3)
 d.rectangle((8,1,14,7),fill='#d9e2e6',outline='#334a59');d.rectangle((10,3,12,5),fill=color)
 if name=='door_link':d.rectangle((11,9,14,14),outline=color);d.point((13,12),fill='#ffffff')
 else:d.line((8,11,11,8,14,11),fill=color,width=1);d.rectangle((9,11,13,14),outline=color)
 im.save(ASSETS/f'textures/item/{name}.png')
 write(ASSETS/f'models/item/{name}.json',{'parent':'minecraft:item/handheld','textures':{'layer0':f'homesystems:item/{name}'}})
 write(DATA/f'homesystems/recipes/{name}.json',{'type':'minecraft:crafting_shaped','category':'redstone','pattern':[' IG',' RI','R  '],'key':{'I':{'item':'minecraft:iron_ingot'},'R':{'item':'minecraft:redstone'},'G':{'item':ingredient}},'result':{'item':f'homesystems:{name}'}})
write(ASSETS/'lang/ko_kr.json',ko);write(ASSETS/'lang/en_us.json',en)
ko['tooltip.homesystems.door_link']='내부 기기·로비폰 선택 → 연결할 문 우클릭'
en['tooltip.homesystems.door_link']='Select indoor device/lobby phone, then right-click a door'
ko['tooltip.homesystems.home_link']='내부 기기 선택 → 도어카메라·도어폰 우클릭'
en['tooltip.homesystems.home_link']='Select indoor device, then right-click door camera/phone'
ko['tooltip.homesystems.link_unpair']='선택 후 대상에 Shift+우클릭: 연결 해제'
en['tooltip.homesystems.link_unpair']='After selecting source, Shift+right-click target to unlink'
write(ASSETS/'lang/ko_kr.json',ko);write(ASSETS/'lang/en_us.json',en)
# Sibling model selector: family catalogs are supplied with actual product assets.
ko.update({'item.homesystems.sibling_selector':'형제모델 선택기','tooltip.homesystems.sibling_selector':'형제모델이 있는 제품을 우클릭하여 모델 선택','screen.homesystems.siblings':'형제모델 선택','screen.homesystems.sibling_hint':'현재 모델은 비활성 표시 · 선택 즉시 적용','message.homesystems.no_siblings':'[홈시스템] 이 제품에는 등록된 형제모델이 없습니다.','message.homesystems.sibling_busy':'[홈시스템] 통화를 종료한 뒤 모델을 변경하세요.','message.homesystems.sibling_changed':'[홈시스템] 형제모델을 %s(으)로 변경했습니다.'})
en.update({'item.homesystems.sibling_selector':'Sibling Model Selector','tooltip.homesystems.sibling_selector':'Right-click a product with registered sibling models','screen.homesystems.siblings':'Select sibling model','screen.homesystems.sibling_hint':'Current model disabled - click to apply','message.homesystems.no_siblings':'[Home Systems] This product has no registered sibling models.','message.homesystems.sibling_busy':'[Home Systems] End the call before changing models.','message.homesystems.sibling_changed':'[Home Systems] Model changed to %s.'})
im=Image.new('RGBA',(16,16),(0,0,0,0));d=ImageDraw.Draw(im)
d.rounded_rectangle((1,3,10,12),radius=1,fill='#c8dce3',outline='#294956');d.rectangle((3,5,8,8),fill='#427f9d')
d.rounded_rectangle((6,5,14,14),radius=1,fill='#e9d5b6',outline='#5b4539');d.rectangle((8,7,12,10),fill='#76ada5');d.line((10,1,14,1,14,3),fill='#65de8c',width=1);d.point((13,2),fill='#65de8c')
im.save(ASSETS/'textures/item/sibling_selector.png')
write(ASSETS/'models/item/sibling_selector.json',{'parent':'minecraft:item/generated','textures':{'layer0':'homesystems:item/sibling_selector'}})
write(DATA/'homesystems/recipes/sibling_selector.json',{'type':'minecraft:crafting_shaped','category':'redstone','pattern':[' Q ','RCR',' I '],'key':{'Q':{'item':'minecraft:quartz'},'R':{'item':'minecraft:redstone'},'C':{'item':'minecraft:comparator'},'I':{'item':'minecraft:iron_ingot'}},'result':{'item':'homesystems:sibling_selector'}})
write(ASSETS/'lang/ko_kr.json',ko);write(ASSETS/'lang/en_us.json',en)
