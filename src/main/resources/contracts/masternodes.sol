pragma solidity ^0.4.24;

contract Masternodes {

    /**
    Data types and constants
    **/
    uint256 public constant maxRewardRatio = 1000000;
    uint256 public constant maxRewardHistoryHops = 64;
    uint256 public constant minimumCollateral = 10000000 trx;

    struct Masternode {
        address ownerAuthAddress;
        address operatorAuthAddress;
        address ownerRewardAddress;
        address operatorRewardAddress;

        uint256 collateralAmount;
        uint256 operatorRewardRatio;

        // The block in which the tx-announce Master node was included.
        uint256 announcementBlockNumber;

        // Formula:
        // <Announcement block number> + max(round_up(<whole collateral> / <10000000 trx>), minBlocksBeforeMnActivation)
        uint256 minActivationBlockNumber;
        // The block in which the tx-activate Master node was included
        uint256 activationBlockNumber;
        // last block number in which payment was requested
        uint256 prevRewardBlockNumber;
 }

    // stores coutners for each block, so masternode's reward could be calculated for past blocks
    struct MasternodesCountersHistory {
        uint256 blockNumber;
        uint256 numOfActivatedMasternodes;
        uint256 wholeActivatedCollateral;
        uint256 rewardsPerBlock;
    }

    mapping(address => uint256) public mnOwnerIndexes; // ownerAuthAddress -> Masternode index
    mapping(address => uint256) public mnOperatorIndexes; // operatorAuthAddress -> Masternode index

    Masternode[] public masternodesArray; // Masternode index -> Masternode
    MasternodesCountersHistory[] public mnsHistory;

    uint256 public wholeCollateral;
    uint256 public wholeActivatedCollateral;
    uint256 public numOfActivatedMasternodes;
    uint256 public minBlocksBeforeMnActivation;
    uint256 public currentRewardsPerBlock;

    constructor(uint256 _minBlocksBeforeMnActivation, uint256 _currentRewardsPerBlock) public {
        minBlocksBeforeMnActivation = _minBlocksBeforeMnActivation;
        currentRewardsPerBlock = _currentRewardsPerBlock;
    }

    /**
    Getters
    **/

    function getMasternodesNum() public view returns (uint256) {
        if (masternodesArray.length == 0) {
            return 0;
        }
        return masternodesArray.length - 1; // first element is "empty" marker
    }

    function getMasternodesHistorySize() public view returns (uint256) {
        return mnsHistory.length;
    }

    /**
    Announce & Activate & Resign master node.
    **/

    function _divCeil(uint256 a, uint256 b) private pure returns (uint) {
        uint256 floor = a / b;
        uint256 mod = a % b;
        if (mod != 0)
            return floor + 1;
        return floor;
    }

    /// Become a master node.
    function announceMasternode(
        address ownerRewardAddress,
        address operator,
        address operatorRewardAddress,
        uint256 operatorRewardRatio
    ) public payable {
        uint256 amount = msg.value;
        address owner = msg.sender;

        require(amount >= minimumCollateral, "trx amount too small");
        require(operatorRewardRatio <= maxRewardRatio, "operator reward ratio too big");

        // sanity checks
        require(ownerRewardAddress != address(0), "owner reward address is empty");
        require(operatorRewardAddress != address(0), "operator reward address is empty");
        require(owner != address(0), "owner address is empty");
        require(operator != address(0), "operator address is empty");

        // auth addresses are unique
        require(owner != operator, "operator address equal owner address");
        require(mnOwnerIndexes[owner] == 0, "owner already exists");
        require(mnOwnerIndexes[operator] == 0, "operator already exists");
        require(mnOperatorIndexes[owner] == 0, "owner already exists");
        require(mnOperatorIndexes[operator] == 0, "operator already exists");

        // If it's first masternode, add 1 empty element in masternodesArray.
        // Because we mark masternode with 0-index as not existing
        if (masternodesArray.length == 0) {
            masternodesArray.length += 1;
        }

        // allocate memory
        uint256 index = masternodesArray.length;
        masternodesArray.length += 1;
        Masternode storage masternode = masternodesArray[index];

        masternode.ownerRewardAddress = ownerRewardAddress;
        masternode.ownerAuthAddress = owner;
        masternode.collateralAmount = amount;
        masternode.operatorAuthAddress = operator;
        masternode.operatorRewardAddress = operatorRewardAddress;
        masternode.operatorRewardRatio = operatorRewardRatio;
        masternode.announcementBlockNumber = block.number;

        // Calc minActivationBlockNumber
        uint256 activationWait = _divCeil(wholeActivatedCollateral, minimumCollateral);
        if (activationWait < minBlocksBeforeMnActivation)
            activationWait = minBlocksBeforeMnActivation;
        masternode.minActivationBlockNumber = block.number + activationWait;

        // insert masterFnode index
        mnOwnerIndexes[owner] = index;
        mnOperatorIndexes[operator] = index;

        // increment wholeCollateral
        wholeCollateral += amount;
    }

    /// Set operator addresses and reward. If masternode already has unspent reward,
    /// it operator reward amount will be recalculated.
    /// Checks:
    ///     Authenticate sender
    /// @param operatorRewardRatio , min is 0, max is maxRewardRatio
    /// Issued by owner
    function setOperator(
        address operator,
        address operatorRewardAddress,
        uint256 operatorRewardRatio
    ) public {
        address owner = msg.sender;

        require(mnOwnerIndexes[owner] != 0, "owner not exists");
        require(operatorRewardRatio <= maxRewardRatio, "operator reward ratio too big");

        Masternode storage masternode = masternodesArray[mnOwnerIndexes[owner]];

        // auth addresses are unique
        require(owner != operator, "operator address equal owner address");
        require(mnOwnerIndexes[operator] == 0, "operator already exists");
        require(mnOperatorIndexes[operator] == 0 || operator == masternode.operatorAuthAddress, "bad operator address");

        // rm old operator auth index
        delete(mnOperatorIndexes[masternode.operatorAuthAddress]);
        // insert new index
        mnOperatorIndexes[operator] = mnOwnerIndexes[owner];

        masternode.operatorAuthAddress = operator;
        masternode.operatorRewardAddress = operatorRewardAddress;
        masternode.operatorRewardRatio = operatorRewardRatio;
    }

    function updateMnsHistory(
        uint256 _blockNumber,
        uint256 _wholeActivatedCollateral,
        uint256 _numOfActivatedMasternodes,
        uint256 _rewardsPerBlock
    ) private {
        if (mnsHistory.length == 0 || mnsHistory[mnsHistory.length - 1].blockNumber != _blockNumber) {
            // insert new history element
            mnsHistory.length += 1;
            mnsHistory[mnsHistory.length - 1].blockNumber = _blockNumber;
        }
        mnsHistory[mnsHistory.length - 1].wholeActivatedCollateral = _wholeActivatedCollateral;
        mnsHistory[mnsHistory.length - 1].numOfActivatedMasternodes = _numOfActivatedMasternodes;
        mnsHistory[mnsHistory.length - 1].rewardsPerBlock = _rewardsPerBlock;
    }

    /// To activate the masternode, masternode node automatically sends this tx,
    /// if passed enough blocks since announcement
    /// Checks:
    ///     passed enough blocks since announcement.
    /// Issued by not activated operator
    function activateMasternode() public {
        address operator = msg.sender;

        require(mnOperatorIndexes[operator] != 0, "operator not exists");

        Masternode storage masternode = masternodesArray[mnOperatorIndexes[operator]];

        require(masternode.activationBlockNumber == 0, "not activated yet");
        require(block.number >= masternode.minActivationBlockNumber, "not passed enough blocks");

        masternode.activationBlockNumber = block.number;
        masternode.prevRewardBlockNumber = block.number;

        // increment activated counters
        wholeActivatedCollateral += masternode.collateralAmount;
        numOfActivatedMasternodes += 1;
        updateMnsHistory(block.number, wholeActivatedCollateral, numOfActivatedMasternodes, currentRewardsPerBlock);
    }

    /// To stop being masternode and refund collateral, user sends tx-Resign
    /// Checks:
    ///     Authenticate sender
    ///     Not in proposal payments period (if has proposal votes)
    /// Issued by owner
    function resign() public {
        address owner = msg.sender;

        require(mnOwnerIndexes[owner] != 0, "owner not exists"); // exists

        destructMn(owner, false);
    }

    function isUint128Less(uint256 amount) private pure returns(bool) {
        return amount < 0x00000000000000000000000000000000ffffffffffffffffffffffffffffffff;
    }

    function getMasternodeRewardPerBlock(
        uint256 collateralAmount,
        uint256 _wholeActivatedCollateral,
        uint256 _rewardsPerBlock
    ) public pure returns(uint256) {
        assert(_wholeActivatedCollateral > 0);
        assert(collateralAmount <= _wholeActivatedCollateral);
        assert(isUint128Less(collateralAmount)); // COIN < uint128
        assert(isUint128Less(_rewardsPerBlock)); // COIN < uint128
        assert(isUint128Less(_wholeActivatedCollateral)); // COIN < uint128

        // (uint128 * uint128) / uint128 <= uint256
        return (_rewardsPerBlock * collateralAmount) / _wholeActivatedCollateral;
    }

    function getOperatorReward(
        uint256 wholeReward,
        uint256 operatorRewardRatio
    ) public pure returns(uint256) {
        assert(maxRewardRatio > 0);
        assert(operatorRewardRatio <= maxRewardRatio);
        assert(isUint128Less(wholeReward)); // COIN < uint256

        // (uint256 * uint256) / uint256 <= uint256
        return (wholeReward * uint(operatorRewardRatio)) / uint(maxRewardRatio);
    }

    function getMasternodeByOwner(
        address _owner
    ) private view returns (Masternode memory masternode) {
        return masternodesArray[mnOwnerIndexes[_owner]];
    }

    function getMasternodeByOperator(
        address _operator
    ) private view returns (Masternode memory masternode) {
        return masternodesArray[mnOperatorIndexes[_operator]];
    }

    function calcBlocks(
        uint256 _from,
        uint256 _to,
        uint256 intervalStartPos,
        uint256 intervalEndPos,
        uint256 collateralAmount,
        uint256 historyLen,
        uint256 index
    ) private view returns(
        uint256 reward,
        uint256 payedUntil,
        bool ok
    ) {
        if (intervalEndPos <= intervalStartPos) {
            return (uint256(0), uint256(0), false);
        }

        uint256 to = _to;
        uint256 from = _from;

        if (to <= from) {
            return (uint256(0), uint256(0), false);
        }

        // Check that (from, to) and (intervalStartPos, intervalEndPos) do overlap
        if ((to <= intervalStartPos) || (from >= intervalEndPos)) {
            return (uint256(0), uint256(0), false);
        }

        if (to > intervalEndPos)
            to = intervalEndPos;
        if (from < intervalStartPos)
            from = intervalStartPos;

        uint256 rewardPerBlock = getMasternodeRewardPerBlock(
            collateralAmount,
            mnsHistory[historyLen - index].wholeActivatedCollateral,
            mnsHistory[historyLen - index].rewardsPerBlock);

        reward = rewardPerBlock * (to - from);
        payedUntil = from;

        return (reward, payedUntil, true);
    }

    /// @return owner reward amount, operator reward amount
    function getMasternodeRewardInfo(
        address owner,
        uint256 rewardHeight
    ) public view returns(uint256 operatorReward, uint256 ownerReward) {
        Masternode memory masternode = getMasternodeByOwner(owner);

        require(masternode.activationBlockNumber > 0, "owner is activated");

        uint256 historyLen = mnsHistory.length;
        uint256 historyHops = historyLen;
        if (historyHops > maxRewardHistoryHops)
            historyHops = maxRewardHistoryHops;

        uint256 wholeReward = 0;
        uint256 payedUntil = rewardHeight;
        for (uint256 i = 1; i <= historyHops; i++) {
            (uint256 reward, uint256 until, bool ok) = calcBlocks(
                    masternode.prevRewardBlockNumber,
                    rewardHeight,
                    mnsHistory[historyLen - i].blockNumber,
                    payedUntil,
                    masternode.collateralAmount,
                    historyLen,
                    i);
                if (!ok) {
                    continue;
                }

                wholeReward += reward;
                payedUntil = until;
        }

        operatorReward = getOperatorReward(wholeReward, masternode.operatorRewardRatio);
        ownerReward = wholeReward - operatorReward;

        return (ownerReward, operatorReward);
    }

    /// To claim MN's reward, this tx is automatically sent
    /// Checks:
    ///     Authenticate sender
    /// Issued by activated operator
    function claimMasternodeReward() public {
        address operator = msg.sender;
        require(mnOperatorIndexes[operator] != 0, "operator not exists");

        Masternode storage masternode = masternodesArray[mnOperatorIndexes[operator]];

        require(masternode.activationBlockNumber > 0, "operator is activated");

        (uint256 ownerReward, uint256 operatorReward) = getMasternodeRewardInfo(
            masternode.ownerAuthAddress,
            block.number);

        masternode.prevRewardBlockNumber = block.number;

        // It's important that we transfer after updating prevRewardBlockNumber (protection against Re-Entrancy)
        masternode.ownerRewardAddress.transfer(ownerReward);
        masternode.operatorRewardAddress.transfer(operatorReward);
    }

    /**
    Master node destructor.
    **/
    /// Remove data
    function _removeMn(
        address owner
    ) private {
        uint256 index = mnOwnerIndexes[owner];
        address operator = masternodesArray[index].operatorAuthAddress;

        // Indexes of the last element
        address ownerLast = masternodesArray[masternodesArray.length - 1].ownerAuthAddress;
        address operatorLast = masternodesArray[masternodesArray.length - 1].operatorAuthAddress;

        // Move the last element to the deleted slot
        mnOwnerIndexes[ownerLast] = index;
        mnOperatorIndexes[operatorLast] = index;
        masternodesArray[index] = masternodesArray[masternodesArray.length - 1];
        masternodesArray.length -= 1;

        // rm index
        delete mnOwnerIndexes[owner];
        delete mnOperatorIndexes[operator];
    }

    /// Revert blockchain state
    function destructMn(
        address owner,
        bool dismissed
    ) private {
        Masternode memory masternode = masternodesArray[mnOwnerIndexes[owner]];
        uint256 collateralAmount = masternode.collateralAmount;

        wholeCollateral -= masternode.collateralAmount;
        if (masternode.activationBlockNumber != 0) {
            // decrement activated counters, if MN is activated
            wholeActivatedCollateral -= masternode.collateralAmount;
            numOfActivatedMasternodes -= 1;
            updateMnsHistory(block.number, wholeActivatedCollateral, numOfActivatedMasternodes, currentRewardsPerBlock);
        }

        _removeMn(owner);

        // refund. It's important that we refund after removing (protection against Re-Entrancy)
        if (dismissed) {
            // If dismissed, allow to fail (if it's sending to a contract, which is forbidden)
            // Otherwise, masternode can make itself undesmissable
            bool ignored = owner.send(collateralAmount);
            ignored = false;
        } else {
            owner.transfer(collateralAmount);
        }
    }
}

