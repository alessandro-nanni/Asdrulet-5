import {QRCodeSVG} from 'qrcode.react'

export function QrCodeCard({code}: { code: string }) {
    const joinUrl = `${window.location.origin}/join/${code}`

    return (
        <div className="card qr-card">
            <div className="qr-frame">
                <QRCodeSVG value={joinUrl} size={160} bgColor="transparent" fgColor="currentColor"/>
            </div>
            <p className="muted qr-url">{joinUrl}</p>
        </div>
    )
}
