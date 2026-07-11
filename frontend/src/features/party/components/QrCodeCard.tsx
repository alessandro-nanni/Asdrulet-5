import { QRCodeSVG } from 'qrcode.react'

export function QrCodeCard({ code }: { code: string }) {
  const joinUrl = `${window.location.origin}/join/${code}`

  return (
    <div>
      <QRCodeSVG value={joinUrl} size={180} />
      <p>{joinUrl}</p>
    </div>
  )
}
