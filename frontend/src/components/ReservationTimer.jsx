import { useEffect, useState } from "react";

function ReservationTimer({
    expirationDate,
    onExpired
}) {
    const calculateRemainingSeconds = () => {
        if (!expirationDate) {
            return 0;
        }

        const expiration =
            new Date(expirationDate).getTime();

        const now =
            new Date().getTime();

        return Math.max(
            Math.floor((expiration - now) / 1000),
            0
        );
    };

    const [remainingSeconds, setRemainingSeconds] =
        useState(calculateRemainingSeconds);

    useEffect(() => {
        const intervalId = setInterval(() => {
            const remaining =
                calculateRemainingSeconds();

            setRemainingSeconds(remaining);

            if (remaining <= 0) {
                clearInterval(intervalId);

                if (onExpired) {
                    onExpired();
                }
            }
        }, 1000);

        return () => {
            clearInterval(intervalId);
        };
    }, [expirationDate, onExpired]);

    const minutes =
        Math.floor(remainingSeconds / 60);

    const seconds =
        remainingSeconds % 60;

    return (
        <div
            className={
                remainingSeconds <= 60
                    ? "reservation-timer warning"
                    : "reservation-timer"
            }
        >
            <span>Tiempo restante</span>

            <strong>
                {String(minutes).padStart(2, "0")}
                :
                {String(seconds).padStart(2, "0")}
            </strong>
        </div>
    );
}

export default ReservationTimer;