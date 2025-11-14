#!/bin/bash

# Test script to verify timer fix and demonstrate progress bar

API_BASE="http://localhost:8081/api"

echo "════════════════════════════════════════════════════════════"
echo "  TESTING: Timer Persistence & Progress Bar Feature"
echo "════════════════════════════════════════════════════════════"
echo ""

# Test 1: Verify timer doesn't reset
echo "TEST 1: Verify auction timer persists across requests"
echo "-------------------------------------------------------"
AUCTION_ID=$(curl -s "${API_BASE}/auctions" | jq -r '.data[0].auctionId')
echo "Testing auction: $AUCTION_ID"
echo ""

echo "Request 1 - Getting auction details..."
RESULT1=$(curl -s "${API_BASE}/auctions/${AUCTION_ID}")
END_TIME_1=$(echo "$RESULT1" | jq -r '.data.endTime')
CREATED_TIME_1=$(echo "$RESULT1" | jq -r '.data.createdTime')
DURATION_1=$(echo "$RESULT1" | jq -r '.data.duration')
echo "  endTime: $END_TIME_1"
echo "  createdTime: $CREATED_TIME_1"
echo "  duration: $DURATION_1 ms"
echo ""

sleep 2
echo "Request 2 - Getting same auction after 2 seconds..."
RESULT2=$(curl -s "${API_BASE}/auctions/${AUCTION_ID}")
END_TIME_2=$(echo "$RESULT2" | jq -r '.data.endTime')
CREATED_TIME_2=$(echo "$RESULT2" | jq -r '.data.createdTime')
DURATION_2=$(echo "$RESULT2" | jq -r '.data.duration')
echo "  endTime: $END_TIME_2"
echo "  createdTime: $CREATED_TIME_2"
echo "  duration: $DURATION_2 ms"
echo ""

if [ "$END_TIME_1" == "$END_TIME_2" ] && [ "$CREATED_TIME_1" == "$CREATED_TIME_2" ]; then
    echo "✅ SUCCESS: Timestamps are preserved correctly!"
    echo "   endTime did NOT reset - timer fix is working!"
else
    echo "❌ FAILED: Timestamps changed between requests"
    echo "   This means the timer is still resetting"
fi
echo ""

# Test 2: Calculate progress bar percentage
echo "TEST 2: Progress Bar Calculation"
echo "-------------------------------------------------------"
CURRENT_TIME=$(date +%s)000  # Convert to milliseconds
TIME_REMAINING=$((END_TIME_1 - CURRENT_TIME))
PERCENTAGE=$(echo "scale=2; ($TIME_REMAINING / $DURATION_1) * 100" | bc)

echo "Current time: $CURRENT_TIME ms"
echo "End time: $END_TIME_1 ms"
echo "Time remaining: $TIME_REMAINING ms"
echo "Duration: $DURATION_1 ms"
echo "Progress: ${PERCENTAGE}%"
echo ""

if (( $(echo "$PERCENTAGE > 50" | bc -l) )); then
    echo "🟢 Progress bar should be GREEN (> 50% time left)"
elif (( $(echo "$PERCENTAGE > 20" | bc -l) )); then
    echo "🟡 Progress bar should be YELLOW (20-50% time left)"
else
    echo "🔴 Progress bar should be RED (< 20% time left - ending soon!)"
fi
echo ""

# Test 3: Show all auctions with their progress
echo "TEST 3: All Active Auctions Progress"
echo "-------------------------------------------------------"
ALL_AUCTIONS=$(curl -s "${API_BASE}/auctions" | jq -r '.data[] | "\(.itemName)|\(.endTime)|\(.duration)"')

while IFS='|' read -r name endTime duration; do
    timeLeft=$((endTime - CURRENT_TIME))
    if [ $timeLeft -lt 0 ]; then
        echo "❌ $name - EXPIRED"
    else
        percent=$(echo "scale=1; ($timeLeft / $duration) * 100" | bc)
        hours=$((timeLeft / 3600000))
        minutes=$(((timeLeft % 3600000) / 60000))
        
        if (( $(echo "$percent > 50" | bc -l) )); then
            echo "🟢 $name - ${percent}% (${hours}h ${minutes}m remaining)"
        elif (( $(echo "$percent > 20" | bc -l) )); then
            echo "🟡 $name - ${percent}% (${hours}h ${minutes}m remaining)"
        else
            echo "🔴 $name - ${percent}% (${hours}h ${minutes}m remaining) ⚠️ ENDING SOON"
        fi
    fi
done <<< "$ALL_AUCTIONS"
echo ""

echo "════════════════════════════════════════════════════════════"
echo "  Frontend Preview"
echo "════════════════════════════════════════════════════════════"
echo ""
echo "To see the visual progress bar:"
echo "  1. Open: http://localhost:5173"
echo "  2. Click on any auction"
echo "  3. Observe the color-coded progress bar below the price"
echo ""
echo "To see progress bar examples:"
echo "  open Frontend-1/auction-client/progress-bar-preview.html"
echo ""
echo "Progress Bar Colors:"
echo "  🟢 Green  = > 50% time remaining (plenty of time)"
echo "  🟡 Yellow = 20-50% time remaining (getting close)"
echo "  🔴 Red    = < 20% time remaining (ending soon!)"
echo "  ⚫ Empty  = 0% time remaining (expired)"
echo ""
echo "════════════════════════════════════════════════════════════"
