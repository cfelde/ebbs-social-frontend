class EbbsHandler {
    constructor(web3, forum) {
        this.web3 = web3;
        this.forum = forum;
    }

    deflateContent(content) {
        let pako = window.pako;
        return Array.from(pako.deflate(JSON.stringify(content)));
    }

    inflateContent(byteContent) {
        let pako = window.pako;
        return byteContent == null ? {} : JSON.parse(pako.inflate(byteContent, {to: "string"}));
    }

    inflateHexContent(hexContent) {
        let pako = window.pako;
        return hexContent == null ? {} : JSON.parse(pako.inflate(this.web3.utils.hexToBytes(hexContent), {to: "string"}));
    }

    async deployForum(abi, bytecode, forumData, forumMeta) {
        let accounts = await this.web3.eth.getAccounts();
        let contract = new this.web3.eth.Contract(abi, null, {data: bytecode});
        let avgGasPrice = await this.web3.eth.getGasPrice();

        /*
        // Using estimateGas sometimes gives me odd errors..
        let gasEstimate = await contract
            .deploy({arguments: [this.deflateContent(forumData), this.deflateContent(forumMeta), true]})
            .estimateGas();
         */
        let gasEstimate = 5000000;

        let instance = await contract
            .deploy({arguments: [this.deflateContent(forumData), this.deflateContent(forumMeta), true]})
            .send({
                from: accounts[0],
                gasPrice: avgGasPrice * 3, // Bump this so that things don't take forever by default..
                gas: gasEstimate + 70000 // Bump up gasEstimate because it seems a bit low..
            });

        return instance;
    }

    async createPost(inReplyTo, postData) {
        let accounts = await this.web3.eth.getAccounts();
        let avgGasPrice = await this.web3.eth.getGasPrice();

        /*
        // Using estimateGas sometimes gives me odd errors..
        let gasEstimate = await this.forum.methods
            .createPost(inReplyTo, this.deflateContent(postData))
            .estimateGas();
         */
        let gasEstimate = 400000;

        let result = await this.forum.methods
            .createPost(inReplyTo, this.deflateContent(postData))
            .send({
                from: accounts[0],
                gasPrice: avgGasPrice * 3, // Bump this so that things don't take forever by default..
                gas: gasEstimate
            });

        return parseInt(result.events.Posted.returnValues.postId);
    }

    async updatePost(postId, postData) {
        let accounts = await this.web3.eth.getAccounts();
        let avgGasPrice = await this.web3.eth.getGasPrice();

        /*
        // Using estimateGas sometimes gives me odd errors..
        let gasEstimate = await this.forum.methods
            .updatePost(postId, this.deflateContent(postData))
            .estimateGas();
         */
        let gasEstimate = 400000;

        await this.forum.methods
            .updatePost(postId, this.deflateContent(postData))
            .send({
                from: accounts[0],
                gasPrice: avgGasPrice * 3, // Bump this so that things don't take forever by default..
                gas: gasEstimate
            });
    }

    async updateMeta(postId, postMeta) {
        let accounts = await this.web3.eth.getAccounts();
        let avgGasPrice = await this.web3.eth.getGasPrice();

        let post = await this.forum.methods.getPost(postId).call();

        /*
        // Using estimateGas sometimes gives me odd errors..
        let gasEstimate = await this.forum.methods
            .setMeta(postId, post.postMeta == null ? [] : this.web3.utils.hexToBytes(post.postMeta), this.deflateContent(postMeta))
            .estimateGas();
         */
        let gasEstimate = 170000;

        await this.forum.methods
            .setMeta(postId, post.postMeta == null ? [] : this.web3.utils.hexToBytes(post.postMeta), this.deflateContent(postMeta))
            .send({
                from: accounts[0],
                gasPrice: avgGasPrice * 3, // Bump this so that things don't take forever by default..
                gas: gasEstimate
            });
    }

    async setModStatus(postId, modStatus) {
        let accounts = await this.web3.eth.getAccounts();
        let avgGasPrice = await this.web3.eth.getGasPrice();

        let post = await this.forum.methods.getPost(postId).call();
        let postMeta = this.inflateHexContent(post.postMeta);

        postMeta["m"] = modStatus === true ? 1 : 0;

        /*
        // Using estimateGas sometimes gives me odd errors..
        let gasEstimate = await this.forum.methods
            .setMeta(postId, post.postMeta == null ? [] : this.web3.utils.hexToBytes(post.postMeta), this.deflateContent(postMeta))
            .estimateGas();
         */
        let gasEstimate = 70000;

        await this.forum.methods
            .setMeta(postId, post.postMeta == null ? [] : this.web3.utils.hexToBytes(post.postMeta), this.deflateContent(postMeta))
            .send({
                from: accounts[0],
                gasPrice: avgGasPrice * 3, // Bump this so that things don't take forever by default..
                gas: gasEstimate
            });
    }

    async vote(postId, points) {
        let accounts = await this.web3.eth.getAccounts();
        let avgGasPrice = await this.web3.eth.getGasPrice();

        /*
        // Using estimateGas sometimes gives me odd errors..
        let gasEstimate = await this.forum.methods
            .vote(postId, points)
            .estimateGas();
         */
        let gasEstimate = 100000;

        await this.forum.methods
            .vote(postId, points)
            .send({
                from: accounts[0],
                gasPrice: avgGasPrice * 3, // Bump this so that things don't take forever by default..
                gas: gasEstimate
            });
    }

    async getUserDetails(userAddress) {
        let adminStatus = await this.forum.methods.isAdmin(userAddress).call();
        let authorPoints = await this.forum.methods.getAuthorPoints(userAddress).call();

        return {
            "isAdmin": (adminStatus & 0x1) === 0x1,
            "isMod": (adminStatus & 0x2) === 0x2,
            "karma": parseInt(authorPoints)
        }
    }

    async getPost(postId) {
        let accounts = await this.web3.eth.getAccounts();
        let post = await this.forum.methods.getPost(postId).call();
        let selfVote = await this.forum.methods.getVoteOnPost(postId, accounts[0]).call();

        return {
            "postId": postId,
            "author": post.author,
            "timestamp": parseInt(post.timestamp) * 1000,
            "inReplyTo": parseInt(post.inReplyTo),
            "replyCounter": parseInt(post.replyCounter),
            "pointCounter": parseInt(post.pointCounter),
            "postData": this.inflateHexContent(post.postData),
            "postMeta": this.inflateHexContent(post.postMeta),
            "vote": parseInt(selfVote)
        }
    }

    async eventToPost(accounts, event) {
        let post = event.returnValues;
        let postId = parseInt(post.postId);

        let blockNumber = event.blockNumber;
        let timestamp = (await this.web3.eth.getBlock(blockNumber)).timestamp;

        let voteEvents = await this.forum.getPastEvents("Voted", {filter: {postId: postId}, fromBlock: blockNumber, toBlock: 'latest'});
        let replyEvents = await this.forum.getPastEvents("Posted", {filter: {inReplyTo: postId}, fromBlock: blockNumber, toBlock: 'latest'});
        let replyEventsIds = new Set(replyEvents.map(e => e.returnValues.postId));

        // Sort by block number high to low..
        voteEvents.sort((a,b) => b.blockNumber - a.blockNumber);

        let selfVote = await this.forum.methods.getVoteOnPost(postId, accounts[0]).call();

        let pointCounter = 1; // All posts start with 1 point..

        if (voteEvents.length > 0)
            pointCounter = parseInt(voteEvents[0].returnValues.newTotalPoints);

        return {
            "postId": postId,
            "author": post.author,
            "timestamp": timestamp * 1000,
            "inReplyTo": parseInt(post.inReplyTo),
            "replyCounter": replyEventsIds.size,
            "pointCounter": pointCounter,
            "postData": this.inflateHexContent(post.postData),
            "postMeta": this.inflateHexContent(post.postMeta),
            "vote": parseInt(selfVote)
        }
    }

    async getLatestPostsByReplyId(inReplyToPostId, millisBack) {
        console.debug("Starting getLatestPostsByReplyId with inReplyToPostId " + inReplyToPostId + " and millisBack " + millisBack);
        let accounts = await this.web3.eth.getAccounts();

        let lastBlockNumber = await this.web3.eth.getBlockNumber();
        let lastTimestamp = (await this.web3.eth.getBlock(lastBlockNumber)).timestamp;

        let firstBlockNumber = lastBlockNumber;
        let firstTimestamp;

        do {
            firstBlockNumber = Math.max(0, firstBlockNumber - 1920);
            firstTimestamp = (await this.web3.eth.getBlock(firstBlockNumber)).timestamp;

            console.debug("Finding block diff using " + firstBlockNumber + " -> " + lastBlockNumber + ": " + ((lastTimestamp - firstTimestamp) * 1000));
        } while ((lastTimestamp - firstTimestamp) * 1000 < millisBack && firstBlockNumber > 0);

        console.debug("getLatestPostsByReplyId between " + firstBlockNumber + " and " + lastBlockNumber + " for post in reply to " + inReplyToPostId);

        let events = await this.forum.getPastEvents("Posted", {filter: {inReplyTo: "" + inReplyToPostId}, fromBlock: firstBlockNumber, toBlock: lastBlockNumber});

        events.sort((a, b) => a.blockNumber - b.blockNumber);

        let posts = await Promise.all(events.map(e => this.eventToPost(accounts, e)));

        // Keep only latest of each postId. We might have duplicate postIds if a post was updated..
        posts = Object.values(posts.reduce((state, post) => {state[post.postId] = post; return state}, {}));
        posts.sort((a, b) => a.postId - b.postId);

        console.debug("Found " + posts.length + " posts from events");

        // If not enough posts found on events, load a few manually..
        // There is a chance we might end up loading the same posts if this
        // is a fairly new forum or one with very little activity. But the
        // idea is that if the forum hasn't had any recent activity we want
        // to display older posts instead..

        if (posts.length >= 10)
            return posts;

        posts = [];

        let postCount = parseInt(await this.forum.methods.getPostCount().call());

        // We can skip post zero because that's loaded manually anyway..
        for (let i = postCount - 1; i > 0 && posts.length < 10; i--) {
            console.debug("Manually loading postId " + i);
            let post = await this.getPost(i);

            if (post.inReplyTo == inReplyToPostId)
                posts.push(post);
        }

        return posts;
    }

    async getLatestPostsOverall(millisBack) {
        console.debug("Starting getLatestPostsOverall with millisBack " + millisBack);
        let accounts = await this.web3.eth.getAccounts();

        let lastBlockNumber = await this.web3.eth.getBlockNumber();
        let lastTimestamp = (await this.web3.eth.getBlock(lastBlockNumber)).timestamp;

        let firstBlockNumber = lastBlockNumber;
        let firstTimestamp;

        do {
            firstBlockNumber = Math.max(0, firstBlockNumber - 1920);
            firstTimestamp = (await this.web3.eth.getBlock(firstBlockNumber)).timestamp;

            console.debug("Finding block diff using " + firstBlockNumber + " -> " + lastBlockNumber + ": " + ((lastTimestamp - firstTimestamp) * 1000));
        } while ((lastTimestamp - firstTimestamp) * 1000 < millisBack && firstBlockNumber > 0);

        console.debug("getLatestPostsOverall between " + firstBlockNumber + " and " + lastBlockNumber);

        let events = await this.forum.getPastEvents("Posted", {fromBlock: firstBlockNumber, toBlock: lastBlockNumber});

        events.sort((a, b) => a.blockNumber - b.blockNumber);

        let posts = await Promise.all(events.map(e => this.eventToPost(accounts, e)));

        // Keep only latest of each postId. We might have duplicate postIds if a post was updated..
        posts = Object.values(posts.reduce((state, post) => {state[post.postId] = post; return state}, {}));
        posts.sort((a, b) => a.postId - b.postId);

        console.debug("Found " + posts.length + " posts from events");

        // If not enough posts found on events, load a few manually..
        // There is a chance we might end up loading the same posts if this
        // is a fairly new forum or one with very little activity. But the
        // idea is that if the forum hasn't had any recent activity we want
        // to display older posts instead..

        if (posts.length >= 10)
            return posts;

        posts = [];

        let postCount = parseInt(await this.forum.methods.getPostCount().call());

        // We can skip post zero because that's loaded manually anyway..
        for (let i = postCount - 1; i > 0 && posts.length < 10; i--) {
            console.debug("Manually loading postId " + i);
            let post = await this.getPost(i);
            posts.push(post);
        }

        return posts;
    }

    async getLatestPostsByAddress(authorAddress, millisBack) {
        console.debug("Starting getLatestPostsByAddress with authorAddress " + authorAddress + " and millisBack " + millisBack);
        let accounts = await this.web3.eth.getAccounts();

        let lastBlockNumber = await this.web3.eth.getBlockNumber();
        let lastTimestamp = (await this.web3.eth.getBlock(lastBlockNumber)).timestamp;

        let firstBlockNumber = lastBlockNumber;
        let firstTimestamp;

        do {
            firstBlockNumber = Math.max(0, firstBlockNumber - 1920);
            firstTimestamp = (await this.web3.eth.getBlock(firstBlockNumber)).timestamp;

            console.debug("Finding block diff using " + firstBlockNumber + " -> " + lastBlockNumber + ": " + ((lastTimestamp - firstTimestamp) * 1000));
        } while ((lastTimestamp - firstTimestamp) * 1000 < millisBack && firstBlockNumber > 0);

        console.debug("getLatestPostsByAddress between " + firstBlockNumber + " and " + lastBlockNumber + " for post by " + authorAddress);

        let events = await this.forum.getPastEvents("Posted", {filter: {author: authorAddress}, fromBlock: firstBlockNumber, toBlock: lastBlockNumber});

        events.sort((a, b) => a.blockNumber - b.blockNumber);

        let posts = await Promise.all(events.map(e => this.eventToPost(accounts, e)));

        // Keep only latest of each postId. We might have duplicate postIds if a post was updated..
        posts = Object.values(posts.reduce((state, post) => {state[post.postId] = post; return state}, {}));
        posts.sort((a, b) => a.postId - b.postId);

        console.debug("Found " + posts.length + " posts from events");

        return posts;
    }

    async getLatestRepliesByAddress(authorAddress, millisBack) {
        console.debug("Starting getLatestRepliesByAddress with authorAddress " + authorAddress + " and millisBack " + millisBack);
        let accounts = await this.web3.eth.getAccounts();

        let lastBlockNumber = await this.web3.eth.getBlockNumber();
        let lastTimestamp = (await this.web3.eth.getBlock(lastBlockNumber)).timestamp;

        let firstBlockNumber = lastBlockNumber;
        let firstTimestamp;

        do {
            firstBlockNumber = Math.max(0, firstBlockNumber - 1920);
            firstTimestamp = (await this.web3.eth.getBlock(firstBlockNumber)).timestamp;

            console.debug("Finding block diff using " + firstBlockNumber + " -> " + lastBlockNumber + ": " + ((lastTimestamp - firstTimestamp) * 1000));
        } while ((lastTimestamp - firstTimestamp) * 1000 < millisBack && firstBlockNumber > 0);

        console.debug("getLatestRepliesByAddress between " + firstBlockNumber + " and " + lastBlockNumber + " for post by " + authorAddress);

        let events = await this.forum.getPastEvents("Posted", {filter: {inReplyToAddress: authorAddress}, fromBlock: firstBlockNumber, toBlock: lastBlockNumber});

        events.sort((a, b) => a.blockNumber - b.blockNumber);

        let posts = await Promise.all(events.map(e => this.eventToPost(accounts, e)));

        // Keep only latest of each postId. We might have duplicate postIds if a post was updated..
        posts = Object.values(posts.reduce((state, post) => {state[post.postId] = post; return state}, {}));
        posts.sort((a, b) => a.postId - b.postId);

        console.debug("Found " + posts.length + " posts from events");

        return posts;
    }

    async getAuthorMeta(authorAddress) {
        let authorPoints = await this.forum.methods.getAuthorPoints(authorAddress).call();

        return {
            "karma": parseInt(authorPoints)
        }
    }

    async getAdmin(index) {
        let adminAddress = await this.forum.methods.getAdmin(index).call();
        let adminStatus = await this.forum.methods.isAdmin(adminAddress).call();

        return {
            "address": adminAddress,
            "adminStatus": parseInt(adminStatus)
        }
    }

    async getAdmins() {
        let adminCount = await this.forum.methods.getAdminCount().call();

        return await Promise.all(Array.from({length: adminCount}, (e, i) => this.getAdmin(i)));
    }

    async saveAdmin(adminAddress, adminStatus) {
        if (!this.web3.utils.isAddress(adminAddress) || !this.web3.utils.checkAddressChecksum(adminAddress)) {
            throw "Invalid admin address";
        }

        let accounts = await this.web3.eth.getAccounts();
        let avgGasPrice = await this.web3.eth.getGasPrice();

        /*
        // Using estimateGas sometimes gives me odd errors..
        let gasEstimate = await this.forum.methods
            .setAdmin(adminAddress, adminStatus)
            .estimateGas();
         */
        let gasEstimate = 100000;

        await this.forum.methods
            .setAdmin(adminAddress, adminStatus)
            .send({
                from: accounts[0],
                gasPrice: avgGasPrice * 3, // Bump this so that things don't take forever by default..
                gas: gasEstimate
            });
    }
}
