import { ImageResponse } from "next/og";

/**
 * Dynamic favicon — editorial "EC" monogram.
 *
 * Ink background, ivory type, gold underscore. Rendered at 32×32 by
 * Next.js on demand; no static asset required.
 */

export const size = { width: 32, height: 32 };
export const contentType = "image/png";

export default function Icon() {
  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          background: "#0A0A0A",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          flexDirection: "column",
          fontFamily: "serif",
        }}
      >
        <div
          style={{
            color: "#F5F1EA",
            fontSize: 18,
            fontWeight: 700,
            letterSpacing: -1,
            lineHeight: 1,
          }}
        >
          EC
        </div>
        <div
          style={{
            width: 10,
            height: 1.5,
            background: "#B8893E",
            marginTop: 3,
          }}
        />
      </div>
    ),
    { ...size }
  );
}
