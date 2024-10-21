let symbols = [];

async function fetchSavedSymbols() {
    try {
        const response = await fetch("/get-symbols");
        symbols = await response.json();

        // 서버에서 받은 symbols가 객체인 경우, 객체의 키(증권 코드)를 정렬
        if (symbols && typeof symbols === 'object') {
            const sortedSymbols = Object.keys(symbols).sort((a, b) => Number(a) - Number(b));
            displaySymbols(sortedSymbols);  // 정렬된 심볼 리스트를 넘김
        }
    } catch (error) {
        console.error("Error fetching symbols:", error);
    }
}

function addSymbol() {
    const symbol = document.getElementById('symbol').value;
    if (symbol && !symbols[symbol]) {
        fetch(`/add-symbol?symbol=${symbol}`, {method: 'POST'})
            .then(response => {
                if (!response.ok) {  // 응답이 성공적이지 않으면 오류 메시지 처리
                    return response.text().then(message => { throw new Error(message); });
                }
                return response.text();
            })
            .then(() => {
                fetchSavedSymbols(); // 추가 후 즉시 리스트를 다시 불러오기
                document.getElementById('symbol').value = '';  // 입력 필드를 초기화
            })
            .catch(error => {
                alert('存在しないコードです。\nもう一度コードを確認してください。');
            });
    }
}

function removeSymbol(symbol) {
    if (confirm('本当にリストから削除しますか?')) {
        fetch(`/remove-symbol?symbol=${symbol}`, {method: 'POST'})
            .then(() => fetchSavedSymbols())  // 삭제 후 즉시 리스트를 다시 불러오기
            .catch(error => console.error('Error removing symbol:', error));
    }
}

async function displaySymbols(sortedSymbols) {
    const symbolList = document.getElementById('symbolList');
    symbolList.innerHTML = '';

    const symbolPromises = sortedSymbols.map(symbol => {
        return fetch(`/get-japanese-name?symbol=${symbol}`)
            .then(response => response.text())
            .then(japaneseName => {
                return {symbol, japaneseName};
            });
    });

    const symbolData = await Promise.all(symbolPromises);
    symbolData.forEach(({symbol, japaneseName}) => {
        symbolList.innerHTML += `<li>${japaneseName} (${symbol}) <button class="delete-btn" onclick="removeSymbol('${symbol}')">削除</button></li>`;
    });
}

function resetSymbols() {
    if (confirm("初期状態にリセットしても宜しいですか？")) {
        fetch("/reset-symbols", {method: 'POST'})
            .then(() => fetchSavedSymbols())
            .catch(error => console.error('Error resetting symbols:', error));
    }
}

window.onload = function () {
    fetchSavedSymbols();
    setDefaultDates();
    // 마지막 검색 결과 불러오기
    fetch('/get-last-search-results')
        .then(response => response.json())
        .then(data => {
            if (data && data.length > 0) {
                displayData(data);  // 마지막 검색 결과 출력
            }
        })
        .catch(error => console.error('Error fetching last search results:', error));
};

function setDefaultDates() {
    const today = new Date();
    const threeDaysAgo = new Date();
    threeDaysAgo.setDate(today.getDate() - 2);

    const todayString = today.toISOString().split('T')[0];
    const threeDaysAgoString = threeDaysAgo.toISOString().split('T')[0];

    document.getElementById('startDate').value = threeDaysAgoString; // 3일 전 날짜를 시작 날짜로 설정
    document.getElementById('endDate').value = todayString;          // 오늘 날짜를 종료 날짜로 설정
}

async function fetchFinancialData() {
    document.getElementById('loading-container').style.display = 'flex';

    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;

    if (!startDate || !endDate || Object.keys(symbols).length === 0) {
        alert('すべての入力値を入力してください。');
        document.getElementById('loading-container').style.display = 'none';
        return;
    }

    const symbolList = Object.keys(symbols).join(',');
    try {
        const response = await fetch(`/financial-data?startDate=${new Date(startDate).getTime() / 1000}&endDate=${new Date(endDate).getTime() / 1000}&symbol=${symbolList}`);
        const data = await response.json();

        if (response.ok) {
            displayData(data);
        } else {
            alert('データの取得に失敗しました。');
        }
    } catch (error) {
        console.error('Error fetching data:', error);
        alert('データの取得に失敗しました。');
    } finally {
        document.getElementById('loading-container').style.display = 'none';
    }
}

// 검색 후 데이터를 서버에 저장
function saveSearchResults(data) {
    fetch('/save-search-results', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(data),
    }).catch(error => console.error('Error saving search results:', error));
}


function displayData(data) {
    const tableBody = document.getElementById('financialData');
    tableBody.innerHTML = '';

    if (!data || data.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="9">データがありません</td></tr>';
        return;
    }

    // 검색 결과를 symbols 배열에 저장 (검색 결과의 전체 데이터를 저장)
    symbols = data;

    data.forEach(entry => {
        const symbol = entry.symbol;
        const secUrl = `https://www.sbisec.co.jp/ETGate/?_ControlID=WPLETsiR001Control&_PageID=WPLETsiR001Idtl30&_DataStoreID=DSWPLETsiR001Control&_ActionID=DefaultAID&s_rkbn=2&s_btype=&i_stock_sec=${symbol}&i_dom_flg=1&i_exchange_code=JPN&i_output_type=2&exchange_code=TKY&stock_sec_code_mul=${symbol}&ref_from=1&ref_to=20`;

        const createCell = (value, isNumeric) => {
            const cellClass = value === '---------' ? 'center-align' : (isNumeric ? 'right-align' : 'center-align');
            return `<td class="${cellClass}">${value}</td>`;
        };

        const companyNameWithSymbol = symbol ? `${entry.shortName} (${symbol})` : entry.shortName;

        const row = `
            <tr>
                <td class="center-align">${entry.date}</td>
                <td class="center-align"><a href="${secUrl}" target="_blank">${companyNameWithSymbol}</a></td>
                ${createCell(entry.open === 0.0 ? "---------" : entry.open.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                ${createCell(entry.price10 === 0.0 ? "---------" : entry.price10.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                ${createCell(entry.price11 === 0.0 ? "---------" : entry.price11.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                ${createCell(entry.price13 === 0.0 ? "---------" : entry.price13.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                ${createCell(entry.price14 === 0.0 ? "---------" : entry.price14.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                ${createCell(entry.close === 0.0 ? "---------" : entry.close.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                <td class="memo-cell" id="memo-${entry.symbol}">${entry.memo || ''}</td>
                <td class="center-align">
                    <button class="edit-btn" onclick="editMemo('${entry.symbol}')">修正</button>
                </td>
            </tr>
        `;
        tableBody.innerHTML += row;
    });

    // 검색 결과를 서버에 저장
    saveSearchResults(data);
}

function editMemo(symbol) {
    const memoCell = document.getElementById(`memo-${symbol}`);
    const currentMemo = memoCell.innerText;

    memoCell.innerHTML = `
        <input type="text" id="memo-input-${symbol}" value="${currentMemo}" />
        <button onclick="saveMemo('${symbol}')">保存</button>
        <button onclick="cancelEdit('${symbol}', '${currentMemo}')">キャンセル</button>
    `;
}

function saveMemo(symbol) {
    const newMemo = document.getElementById(`memo-input-${symbol}`).value;

    // symbols 배열에서 해당 심볼을 찾아 메모를 업데이트
    symbols = symbols.map(entry => {
        if (entry.symbol === symbol) {let symbols = [];

            async function fetchSavedSymbols() {
                try {
                    const response = await fetch("/get-symbols");
                    symbols = await response.json();

                    // 서버에서 받은 symbols가 객체인 경우, 객체의 키(증권 코드)를 정렬
                    if (symbols && typeof symbols === 'object') {
                        const sortedSymbols = Object.keys(symbols).sort((a, b) => Number(a) - Number(b));
                        displaySymbols(sortedSymbols);  // 정렬된 심볼 리스트를 넘김
                    }
                } catch (error) {
                    console.error("Error fetching symbols:", error);
                }
            }

            function addSymbol() {
                const symbol = document.getElementById('symbol').value;
                if (symbol && !symbols[symbol]) {
                    fetch(`/add-symbol?symbol=${symbol}`, {method: 'POST'})
                        .then(response => {
                            if (!response.ok) {  // 응답이 성공적이지 않으면 오류 메시지 처리
                                return response.text().then(message => { throw new Error(message); });
                            }
                            return response.text();
                        })
                        .then(() => {
                            fetchSavedSymbols(); // 추가 후 즉시 리스트를 다시 불러오기
                            document.getElementById('symbol').value = '';  // 입력 필드를 초기화
                        })
                        .catch(error => {
                            alert('存在しないコードです。\nもう一度コードを確認してください。');
                        });
                }
            }

            function removeSymbol(symbol) {
                if (confirm('本当にリストから削除しますか?')) {
                    fetch(`/remove-symbol?symbol=${symbol}`, {method: 'POST'})
                        .then(() => fetchSavedSymbols())  // 삭제 후 즉시 리스트를 다시 불러오기
                        .catch(error => console.error('Error removing symbol:', error));
                }
            }

            async function displaySymbols(sortedSymbols) {
                const symbolList = document.getElementById('symbolList');
                symbolList.innerHTML = '';

                const symbolPromises = sortedSymbols.map(symbol => {
                    return fetch(`/get-japanese-name?symbol=${symbol}`)
                        .then(response => response.text())
                        .then(japaneseName => {
                            return {symbol, japaneseName};
                        });
                });

                const symbolData = await Promise.all(symbolPromises);
                symbolData.forEach(({symbol, japaneseName}) => {
                    symbolList.innerHTML += `<li>${japaneseName} (${symbol}) <button class="delete-btn" onclick="removeSymbol('${symbol}')">削除</button></li>`;
                });
            }

            function resetSymbols() {
                if (confirm("初期状態にリセットしても宜しいですか？")) {
                    fetch("/reset-symbols", {method: 'POST'})
                        .then(() => fetchSavedSymbols())
                        .catch(error => console.error('Error resetting symbols:', error));
                }
            }

            window.onload = function () {
                fetchSavedSymbols();
                setDefaultDates();
                // 마지막 검색 결과 불러오기
                fetch('/get-last-search-results')
                    .then(response => response.json())
                    .then(data => {
                        if (data && data.length > 0) {
                            displayData(data);  // 마지막 검색 결과 출력
                        }
                    })
                    .catch(error => console.error('Error fetching last search results:', error));
            };

            function setDefaultDates() {
                const today = new Date();
                const threeDaysAgo = new Date();
                threeDaysAgo.setDate(today.getDate() - 2);

                const todayString = today.toISOString().split('T')[0];
                const threeDaysAgoString = threeDaysAgo.toISOString().split('T')[0];

                document.getElementById('startDate').value = threeDaysAgoString; // 3일 전 날짜를 시작 날짜로 설정
                document.getElementById('endDate').value = todayString;          // 오늘 날짜를 종료 날짜로 설정
            }

            async function fetchFinancialData() {
                document.getElementById('loading-container').style.display = 'flex';

                const startDate = document.getElementById('startDate').value;
                const endDate = document.getElementById('endDate').value;

                if (!startDate || !endDate || Object.keys(symbols).length === 0) {
                    alert('すべての入力値を入力してください。');
                    document.getElementById('loading-container').style.display = 'none';
                    return;
                }

                const symbolList = Object.keys(symbols).join(',');
                try {
                    const response = await fetch(`/financial-data?startDate=${new Date(startDate).getTime() / 1000}&endDate=${new Date(endDate).getTime() / 1000}&symbol=${symbolList}`);
                    const data = await response.json();

                    if (response.ok) {
                        displayData(data);
                    } else {
                        alert('データの取得に失敗しました。');
                    }
                } catch (error) {
                    console.error('Error fetching data:', error);
                    alert('データの取得に失敗しました。');
                } finally {
                    document.getElementById('loading-container').style.display = 'none';
                }
            }

// 검색 후 데이터를 서버에 저장
            function saveSearchResults(data) {
                fetch('/save-search-results', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(data),
                }).catch(error => console.error('Error saving search results:', error));
            }


            function displayData(data) {
                const tableBody = document.getElementById('financialData');
                tableBody.innerHTML = '';

                if (!data || data.length === 0) {
                    tableBody.innerHTML = '<tr><td colspan="9">データがありません</td></tr>';
                    return;
                }

                // 검색 결과를 symbols 배열에 저장 (검색 결과의 전체 데이터를 저장)
                symbols = data;

                data.forEach(entry => {
                    const symbol = entry.symbol;
                    const secUrl = `https://www.sbisec.co.jp/ETGate/?_ControlID=WPLETsiR001Control&_PageID=WPLETsiR001Idtl30&_DataStoreID=DSWPLETsiR001Control&_ActionID=DefaultAID&s_rkbn=2&s_btype=&i_stock_sec=${symbol}&i_dom_flg=1&i_exchange_code=JPN&i_output_type=2&exchange_code=TKY&stock_sec_code_mul=${symbol}&ref_from=1&ref_to=20`;

                    const createCell = (value, isNumeric) => {
                        const cellClass = value === '---------' ? 'center-align' : (isNumeric ? 'right-align' : 'center-align');
                        return `<td class="${cellClass}">${value}</td>`;
                    };

                    const companyNameWithSymbol = symbol ? `${entry.shortName} (${symbol})` : entry.shortName;

                    const row = `
            <tr>
                <td class="center-align">${entry.date}</td>
                <td class="center-align"><a href="${secUrl}" target="_blank">${companyNameWithSymbol}</a></td>
                ${createCell(entry.open === 0.0 ? "---------" : entry.open.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                ${createCell(entry.price10 === 0.0 ? "---------" : entry.price10.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                ${createCell(entry.price11 === 0.0 ? "---------" : entry.price11.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                ${createCell(entry.price13 === 0.0 ? "---------" : entry.price13.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                ${createCell(entry.price14 === 0.0 ? "---------" : entry.price14.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                ${createCell(entry.close === 0.0 ? "---------" : entry.close.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                <td class="center-align" id="memo-${entry.symbol}">${entry.memo || ''}</td>
                <td class="center-align">
                    <button class="edit-btn" onclick="editMemo('${entry.symbol}')">修正</button>
                </td>
            </tr>
        `;
                    tableBody.innerHTML += row;
                });

                // 검색 결과를 서버에 저장
                saveSearchResults(data);
            }

            function editMemo(symbol) {
                const memoCell = document.getElementById(`memo-${symbol}`);
                const currentMemo = memoCell.innerText;

                memoCell.innerHTML = `
        <input type="text" id="memo-input-${symbol}" value="${currentMemo}" />
        <button onclick="saveMemo('${symbol}')">保存</button>
        <button onclick="cancelEdit('${symbol}', '${currentMemo}')">キャンセル</button>
    `;
            }

            function saveMemo(symbol) {
                const newMemo = document.getElementById(`memo-input-${symbol}`).value;

                // symbols 배열에서 해당 심볼을 찾아 메모를 업데이트
                symbols = symbols.map(entry => {
                    if (entry.symbol === symbol) {
                        entry.memo = newMemo;  // 메모를 업데이트
                    }
                    return entry;
                });

                // 메모 저장 후 전체 데이터를 서버에 저장
                fetch('/save-search-results', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(symbols),  // 수정된 전체 데이터를 서버로 전송
                }).then(response => {
                    if (response.ok) {
                        // 서버 저장이 완료된 후 화면을 업데이트
                        document.getElementById(`memo-${symbol}`).innerHTML = newMemo;  // 수정된 메모를 화면에 표시
                    } else {
                        alert('メモの保存に失敗しました。');
                    }
                }).catch(error => {
                    console.error('メモの保存中にエラーが発生しました:', error);
                });
            }


            function cancelEdit(symbol, originalMemo) {
                document.getElementById(`memo-${symbol}`).innerHTML = originalMemo;
            }

// 자동완성 기능
            async function fetchMatchingSymbols(query) {
                if (query.length < 2) {
                    clearAutocomplete();
                    return;
                }

                try {
                    const response = await fetch(`/search-symbol?query=${encodeURIComponent(query)}`);
                    const data = await response.json();
                    displayAutocomplete(data);
                } catch (error) {
                    console.error("Error fetching matching symbols:", error);
                }
            }

            function displayAutocomplete(symbols) {
                clearAutocomplete();
                const autocompleteList = document.getElementById('autocomplete-list');
                symbols.forEach(symbol => {
                    const item = document.createElement("div");
                    item.innerHTML = `<strong>${symbol.japaneseName}</strong> (${symbol.symbol})`;
                    item.addEventListener("click", function() {
                        document.getElementById('symbol').value = symbol.symbol;
                        clearAutocomplete();
                    });
                    autocompleteList.appendChild(item);
                });
            }

            function clearAutocomplete() {
                const autocompleteList = document.getElementById('autocomplete-list');
                autocompleteList.innerHTML = '';
            }
            entry.memo = newMemo;  // 메모를 업데이트
        }
        return entry;
    });

    // 메모 저장 후 전체 데이터를 서버에 저장
    fetch('/save-search-results', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(symbols),  // 수정된 전체 데이터를 서버로 전송
    }).then(response => {
        if (response.ok) {
            // 서버 저장이 완료된 후 화면을 업데이트
            document.getElementById(`memo-${symbol}`).innerHTML = newMemo;  // 수정된 메모를 화면에 표시
        } else {
            alert('メモの保存に失敗しました。');
        }
    }).catch(error => {
        console.error('メモの保存中にエラーが発生しました:', error);
    });
}


function cancelEdit(symbol, originalMemo) {
    document.getElementById(`memo-${symbol}`).innerHTML = originalMemo;
}

// 자동완성 기능
async function fetchMatchingSymbols(query) {
    if (query.length < 2) {
        clearAutocomplete();
        return;
    }

    try {
        const response = await fetch(`/search-symbol?query=${encodeURIComponent(query)}`);
        const data = await response.json();
        displayAutocomplete(data);
    } catch (error) {
        console.error("Error fetching matching symbols:", error);
    }
}

function displayAutocomplete(symbols) {
    clearAutocomplete();
    const autocompleteList = document.getElementById('autocomplete-list');
    symbols.forEach(symbol => {
        const item = document.createElement("div");
        item.innerHTML = `<strong>${symbol.japaneseName}</strong> (${symbol.symbol})`;
        item.addEventListener("click", function() {
            document.getElementById('symbol').value = symbol.symbol;
            clearAutocomplete();
        });
        autocompleteList.appendChild(item);
    });
}

function clearAutocomplete() {
    const autocompleteList = document.getElementById('autocomplete-list');
    autocompleteList.innerHTML = '';
}